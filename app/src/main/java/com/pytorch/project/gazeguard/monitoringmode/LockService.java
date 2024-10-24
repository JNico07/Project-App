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
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class LockService extends Service {

    private static final String CHANNEL_ID = "LockServiceChannel";
    private static final long LOCK_DURATION = 10000; // Lock every #
    private static long TOTAL_DURATION = 30000; // Run for # seconds
    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;
    private Handler handler;
    private Runnable lockRunnable;
    private CountDownTimer countDownTimer;
    private Context context;
    Intent controlDetectorServiceIntent;

    private static final long DELAY = 5000;

    private String unlockTime;
    private DateTimeFormatter formatterTime;
    private LocalTime currentLocalTime, timeUnlockDevice;

    @Override
    public void onCreate() {
        super.onCreate();

        controlDetectorServiceIntent = new Intent(this, DetectorService.class);
        controlDetectorServiceIntent.setAction("STOP_CAMERA");
        startService(controlDetectorServiceIntent);

        formatterTime = DateTimeFormatter.ofPattern("h:mm a").withLocale(Locale.US);

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        handler = new Handler();
//        lockRunnable = new Runnable() {
//            @Override
//            public void run() {
//                if (devicePolicyManager.isAdminActive(componentName)) {
//                    devicePolicyManager.lockNow();
//                }
//            }
//        };
        // Start the locking process
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startLockingProcess();
            }
        }, DELAY);

        startUpdatingCurrentLocalTime();
    }

    private void startLockingProcess() {
        currentLocalTime = parseCurrentLocalTime();

        Log.d("LockService", "Current Time: " + currentLocalTime);
        Log.d("LockService", "Time Unlock Device: " + timeUnlockDevice);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (shouldUnlockDevice()) {
                    Log.d("LockService", "Device unlocking...");
                    stopSelf();
                } else if (shouldLockDevice()) {
                    lockDevice();
                    Log.d("LockService", "Device locked");
                }
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
    private boolean shouldLockDevice() {
        return currentLocalTime.isBefore(timeUnlockDevice);
    }
    private void lockDevice() {
        if (devicePolicyManager.isAdminActive(componentName)) {
            try {
                // Simply lock the device - calls will still be receivable by default
                devicePolicyManager.lockNow();

                // Stop camera service
                controlDetectorServiceIntent.setAction("STOP_CAMERA");
                startService(controlDetectorServiceIntent);

                Toast.makeText(this, "Device locked - Calls are still receivable", Toast.LENGTH_SHORT).show();
                Log.d("LockService", "Device locked successfully");
            } catch (Exception e) {
                Log.e("LockService", "Error locking device: " + e.getMessage());
                Toast.makeText(this, "Failed to lock device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w("LockService", "Device admin is not active");
            Toast.makeText(this, "Device admin permissions not granted", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean shouldUnlockDevice() {
        return timeUnlockDevice != null && !currentLocalTime.isBefore(timeUnlockDevice);
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
        createNotificationChannel();

        // Fetch the unlock time from the intent (sent by DetectorService)
        if (intent != null) {
            unlockTime = intent.getStringExtra("UNLOCK_TIME");
            if (unlockTime != null && !unlockTime.isEmpty()) {
                try {
                    timeUnlockDevice = LocalTime.parse(unlockTime, formatterTime);
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
        super.onDestroy();

        // Stop any pending callbacks or timers
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;  // Clear the reference
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;  // Clear the reference
        }

        // Clean up device policy manager
        if (devicePolicyManager != null) {
            devicePolicyManager = null;
        }
        componentName = null;

        // Clean up context reference
        context = null;

        // Restart DetectorService or any other cleanup action
        if (controlDetectorServiceIntent != null) {
            controlDetectorServiceIntent.setAction("START_CAMERA");
            startService(controlDetectorServiceIntent);
            controlDetectorServiceIntent = null;  // Clear the reference
        }

        // Remove the ongoing notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(1);  // Use the same notification ID used in startForeground
        }
        stopForeground(true);

        // Log cleanup
        Log.d("LockService", "Device Unlocked!");
        Toast.makeText(getApplicationContext(), "Locking Device Finished!", Toast.LENGTH_LONG).show();
    }
}





