package com.pytorch.project.gazeguard.monitoringmode;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class LockService extends Service {

    private static final String CHANNEL_ID = "LockServiceChannel";
    private static final long LOCK_DURATION = 10000; // Lock every #
    private static final long TOTAL_DURATION = 120000; // Run for # seconds
    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;
    private Handler handler;
    private Runnable lockRunnable;
    private CountDownTimer countDownTimer;

    private static final long INITIAL_DELAY = 20000;

    @Override
    public void onCreate() {
        super.onCreate();

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        Toast.makeText(this, "LockService started", Toast.LENGTH_SHORT).show();

        handler = new Handler();
        lockRunnable = new Runnable() {
            @Override
            public void run() {
                if (devicePolicyManager.isAdminActive(componentName)) {
                    devicePolicyManager.lockNow();
                    handler.postDelayed(this, LOCK_DURATION);
                }
            }
        };
        // Start the locking process with a delay
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startLockingProcess();
            }
        }, INITIAL_DELAY); // 10 seconds delay before first lock
    }

    private void startLockingProcess() {
        // Start a CountDownTimer for the total duration
        countDownTimer = new CountDownTimer(TOTAL_DURATION, LOCK_DURATION) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Lock the device every 5 seconds
                if (devicePolicyManager.isAdminActive(componentName)) {
                    devicePolicyManager.lockNow();
                }
            }

            @Override
            public void onFinish() {
                stopSelf(); // Stop the service once finished
            }
        }.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(lockRunnable);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }
}
