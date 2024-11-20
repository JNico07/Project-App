package com.pytorch.project.gazeguard.monitoringmode;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pytorch.project.gazeguard.common.SharedPrefsUtil;

public class LockService extends Service {

    private static final String CHANNEL_ID = "LockServiceChannel";
    private static final long LOCK_DURATION = 20000; // Lock every #
    private static long TOTAL_DURATION; // Run for # seconds
    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;
    private Handler handler;
    private Runnable lockRunnable;
    private CountDownTimer countDownTimer;
    private Context context;
    Intent controlDetectorServiceIntent;

    private static final long DELAY = 3000;

    private String unlockTime;
    private DateTimeFormatter formatterTime;
    private LocalTime currentLocalTime, timeUnlockDevice;

    private boolean isServiceStopping = false;

    private DatabaseReference deviceUnlockTimeRef;
    private FirebaseAuth mAuth;
    private ValueEventListener unlockTimeListener;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());

        controlDetectorServiceIntent = new Intent(this, DetectorService.class);
        controlDetectorServiceIntent.setAction("STOP_CAMERA");
        startService(controlDetectorServiceIntent);

        controlDetectorServiceIntent.setAction("STOP_TIMER");
        startService(controlDetectorServiceIntent);

        formatterTime = DateTimeFormatter.ofPattern("h:mm a").withLocale(Locale.US);

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyDeviceAdminReceiver.class);

//        lockRunnable = new Runnable() {
//            @Override
//            public void run() {
//                if (!isServiceStopping && devicePolicyManager.isAdminActive(componentName)) {
//                    devicePolicyManager.lockNow();
//                    handler.postDelayed(this, LOCK_DURATION);
//                }
//            }
//        };

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isServiceStopping) {
                    startLockingProcess();
                }
            }
        }, DELAY);

        startUpdatingCurrentLocalTime();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            String childNumber = SharedPrefsUtil.getChildNumber(this);

            // Setup Firebase reference for unlock time
            deviceUnlockTimeRef = FirebaseDatabase.getInstance().getReference()
                    .child("Registered Users")
                    .child(uid)
                    .child("Child")
                    .child(childNumber)
                    .child("deviceUnlockTime");

            // Add listener for unlock time changes
            unlockTimeListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String newUnlockTime = snapshot.getValue(String.class);
                        if (newUnlockTime != null && !newUnlockTime.equals(unlockTime)) {
                            unlockTime = newUnlockTime;
                            try {
                                timeUnlockDevice = LocalTime.parse(unlockTime, formatterTime);
                                calculateRemainingDurationMillis();

                                // Cancel existing timer
                                if (countDownTimer != null) {
                                    countDownTimer.cancel();
                                }

                                // Start new timer with updated duration
                                lockDevice();

                                Log.d("LockService", "Unlock time updated to: " + unlockTime);
                                Log.d("LockService", "New total duration: " + TOTAL_DURATION);
                            } catch (DateTimeParseException e) {
                                Log.e("LockService", "Failed to parse new unlock time: " + unlockTime, e);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("LockService", "Failed to read unlock time", error.toException());
                }
            };

            // Start listening for changes
            deviceUnlockTimeRef.addValueEventListener(unlockTimeListener);
        }
    }

    private void startLockingProcess() {
        currentLocalTime = parseCurrentLocalTime();

        Log.d("LockService", "Current Time: " + currentLocalTime);
        Log.d("LockService", "Time Unlock Device: " + timeUnlockDevice);
        Log.d("LockService", "Total Duration: " + TOTAL_DURATION);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                lockDevice();
                Log.d("LockService", "Device locked");
                handler.postDelayed(this, 10000);
            }
        }, 10000); // Initial delay
    }
    private LocalTime parseCurrentLocalTime() {
        try {
            return LocalTime.parse(getCurrentLocalTime(), formatterTime);
        } catch (DateTimeParseException e) {
            Log.e("LockService", "Failed to parse current time", e);
            return LocalTime.now(); // Set default value or handle the error
        }
    }

    private void lockDevice() {
        if (isServiceStopping) {
            return;
        }

        // check if unlock time is set
        if (timeUnlockDevice == null) {
            TOTAL_DURATION = Long.MAX_VALUE;
        }

        // Start a CountDownTimer for the total duration
        if (TOTAL_DURATION > 0) {
            countDownTimer = new CountDownTimer(TOTAL_DURATION, LOCK_DURATION) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (isServiceStopping) {
                        updateLockStatus(false);
                        cancel();
                        return;
                    }
                    // Lock the device
                    if (devicePolicyManager.isAdminActive(componentName)) {
                        devicePolicyManager.lockNow();
                        updateLockStatus(true);

                        // Stop camera service
                        controlDetectorServiceIntent.setAction("STOP_CAMERA");
                        startService(controlDetectorServiceIntent);

                        Toast.makeText(LockService.this, "LockService running", Toast.LENGTH_SHORT).show();
                        Log.d("LockService", "Device locked successfully");
                    } else {
                        Log.w("LockService", "Device admin is not active");
                        Toast.makeText(LockService.this, "Device admin permissions not granted", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFinish() {
                    if (!isServiceStopping) {
                        updateLockStatus(false);
                        controlDetectorServiceIntent.setAction("START_CAMERA");
                        startService(controlDetectorServiceIntent);
                        stopSelf();
                    }
                }
            }.start();
        }
    }
    private void calculateRemainingDurationMillis() {
        LocalDateTime now = LocalDateTime.now();

        if (timeUnlockDevice != null) {
            LocalDateTime unlockDateTime = timeUnlockDevice.atDate(LocalDate.now());

            // Check if timeUnlockDevice is logically set for the next day
            if (unlockDateTime.isBefore(now)) {
                // Adjust to the next day
                unlockDateTime = unlockDateTime.plusDays(1);
            }

            TOTAL_DURATION = java.time.Duration.between(now, unlockDateTime).toMillis();
        } else {
            // Default TOTAL_DURATION if no unlock time is provided
            TOTAL_DURATION = Integer.MAX_VALUE;
        }

        Log.d("LockService", "Total Duration Calculated: " + TOTAL_DURATION);
    }

    private void startUpdatingCurrentLocalTime() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    currentLocalTime = LocalTime.parse(getCurrentLocalTime(), formatterTime);
                    Log.d("LockService", "Updated Current Time: " + currentLocalTime);
                } catch (DateTimeParseException e) {
                    Log.e("LockService", "Failed to parse current time", e);
                    currentLocalTime = LocalTime.now(); // Fallback to now if parsing fails
                }

                handler.postDelayed(this, 30000);
            }
        }, 30000);
    }
    private String getCurrentLocalTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatterTime);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        createNotificationChannel();
        Log.d("LockService", "onStartCommand called");

        controlDetectorServiceIntent.setAction("STOP_TIMER");
        startService(controlDetectorServiceIntent);

        if (intent != null) {
            unlockTime = intent.getStringExtra("UNLOCK_TIME");
            if (unlockTime != null && !unlockTime.isEmpty()) {
                try {
                    timeUnlockDevice = LocalTime.parse(unlockTime, formatterTime);

                    calculateRemainingDurationMillis();
                    Log.d("LockService", "Total Duration: " + TOTAL_DURATION);

                    int hours = (int) (TOTAL_DURATION / 3600000);
                    int minutes = (int) ((TOTAL_DURATION % 3600000) / 60000);
                    Toast.makeText(LockService.this,
                            "Total Lock Duration: " + hours + " hours " + minutes + " minutes",
                            Toast.LENGTH_LONG).show();
                } catch (DateTimeParseException e) {
                    Log.e("LockService", "Failed to parse unlock time: " + unlockTime, e);
                }
            } else {
                Log.d("LockService", "No unlock time provided");
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lock Service")
                .setContentText("Device will be locked periodically")
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // Start locking process
        startLockingProcess();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Lock Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        isServiceStopping = true;
        updateLockStatus(false);

        try {
            // Remove Firebase listener
            if (deviceUnlockTimeRef != null && unlockTimeListener != null) {
                deviceUnlockTimeRef.removeEventListener(unlockTimeListener);
            }

            // Safely clean up handler
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                if (lockRunnable != null) {
                    handler.removeCallbacks(lockRunnable);
                }
                handler = null;
            }

            // Safely clean up timer
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }

            // Safely restart DetectorService
            if (controlDetectorServiceIntent != null) {
                controlDetectorServiceIntent.setAction("START_CAMERA");
                startService(controlDetectorServiceIntent);
            }

            // Safely remove notification
            try {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(1);
                }
                stopForeground(true);
            } catch (Exception e) {
                Log.e("LockService", "Error removing notification", e);
            }

            // Log cleanup
            Log.d("LockService", "Device Unlocked!");
            Toast.makeText(getApplicationContext(), "Locking Device Finished!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("LockService", "Error in onDestroy", e);
        } finally {
            super.onDestroy();
        }
    }

    private void updateLockStatus(boolean isLocked) {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            String childNumber = SharedPrefsUtil.getChildNumber(this);
            
            DatabaseReference lockStatusRef = FirebaseDatabase.getInstance().getReference()
                    .child("Registered Users")
                    .child(uid)
                    .child("Child")
                    .child(childNumber)
                    .child("isDeviceLocked");
                    
            lockStatusRef.setValue(isLocked);
        }
    }

}



