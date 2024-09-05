package com.pytorch.project.gazeguard.monitoringmode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;

import java.util.Objects;

public class DetectorBroadcastReceiver  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceIntent = new Intent(context, DetectorService.class);
            serviceIntent.setAction("START_TIMER");

            context.startForegroundService(serviceIntent);
        }
    }
}