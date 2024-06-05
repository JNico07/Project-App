package org.tensorflow.lite.examples.detection.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;

import org.tensorflow.lite.examples.detection.DetectorService;
import org.tensorflow.lite.examples.detection.R;

import java.util.Objects;

public class TimerBroadcastReceiver  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceIntent = new Intent(context, DetectorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                if (DetectorService.isRunning) {
                    // Pause the timer
                    serviceIntent.setAction("PAUSE_TIMER");
                } else {
                    // Start the timer
                    serviceIntent.setAction("START_TIMER");
                }

                context.startForegroundService(serviceIntent);
            }
        }
    }
}