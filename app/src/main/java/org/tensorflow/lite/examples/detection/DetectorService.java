package org.tensorflow.lite.examples.detection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetectorService extends Service {

    private Context context;
    private final  int NOTIFICATION_ID = 1;
    private final String  CHANNEL_ID = "100";
    private boolean isDestroyed = false;

    public DetectorService() {
    }


    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private int timerSeconds = 0;
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            timerSeconds++;
            int hours = timerSeconds / 3600;
            int minutes = (timerSeconds % 3600) / 60;
            int seconds = timerSeconds % 60;

            String time = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
            updateNotification(time);

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        startForeground(NOTIFICATION_ID, showNotification("this is content."));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(context, "Time: ", Toast.LENGTH_SHORT).show();
        doTask();
        return super.onStartCommand(intent, flags, startId);
    }

    private void doTask() {
        detector();
    }

    private void updateNotification(String data) {
        Notification notification = showNotification(data);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification showNotification(String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, "Foreground Notification",
                            NotificationManager.IMPORTANCE_HIGH)
            );
        }

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();

        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        Toast.makeText(context, "Stopping Service...", Toast.LENGTH_SHORT).show();
    }



    private void detector() {

        handler.postDelayed(runnable, 1000);

    }


}