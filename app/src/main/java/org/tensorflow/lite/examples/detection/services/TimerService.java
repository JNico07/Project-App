package org.tensorflow.lite.examples.detection.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class TimerService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        while(true) {
                            Log.d("TAG", "Foreground Service is Running...");

                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {

                            }
                        }
                    }
                }
        ) .start();

        final String CHANNEL_IO = "Foreground Service";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_IO, CHANNEL_IO, NotificationManager.IMPORTANCE_LOW);


            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification.Builder notifiation = new Notification.Builder(this, CHANNEL_IO)
                    .setContentText("Foreground Service running")
                    .setContentTitle("This is TITLE");
            startForeground(1001, notifiation.build());

        }


        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
