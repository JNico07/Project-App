package com.pytorch.project.gazeguard.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pytorch.project.gazeguard.parentdashboard.ParentDashboardActivity;
import org.pytorch.demo.objectdetection.R;

public class DeviceLockNotificationService {
    private static final String CHANNEL_ID = "device_lock_channel";
    private static ValueEventListener lockStatusListener;
    private static NotificationManager notificationManager;
    
    // Generate unique notification ID for each child
    private static int getNotificationId(String childId) {
        return childId.hashCode();
    }
    
    public static void startListeningForLockStatus(Context context) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference lockStatusRef = FirebaseDatabase.getInstance().getReference()
                .child("Registered Users")
                .child(uid)
                .child("Child");

        createNotificationChannel(context);

        lockStatusListener = lockStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("NotificationService", "Data changed: " + snapshot.toString());
                
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String childId = childSnapshot.getKey();
                    String childName = childSnapshot.child("name").getValue(String.class);
                    Boolean isLocked = childSnapshot.child("isDeviceLocked").getValue(Boolean.class);
                    
                    Log.d("NotificationService", "Child: " + childName + ", Locked: " + isLocked);
                    
                    if (isLocked != null && childName != null && childId != null) {
                        int notificationId = getNotificationId(childId);
                        if (isLocked) {
                            Log.d("NotificationService", "Showing notification for " + childName);
                            showDeviceLockNotification(context, childName, notificationId);
                        } else {
                            Log.d("NotificationService", "Cancelling notification for " + childName);
                            notificationManager.cancel(notificationId);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LockNotification", "Failed to read lock status", error.toException());
            }
        });
    }

    private static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Device Lock Notifications",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications for device lock status");
        channel.enableLights(true);
        channel.enableVibration(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    private static void showDeviceLockNotification(Context context, String childName, int notificationId) {
        Intent intent = new Intent(context, ParentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId, // Use unique request code
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_important)
                .setContentTitle("Device Locked")
                .setContentText(childName + "'s reached Screen Time Limit, device has been locked")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setOnlyAlertOnce(false);

        if (notificationManager != null) {
            Log.d("NotificationService", "Notification being shown for " + childName);
            notificationManager.notify(notificationId, builder.build());
        } else {
            Log.e("NotificationService", "NotificationManager is null");
        }
    }

    public static void stopListening() {
        if (lockStatusListener != null) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() != null) {
                String uid = mAuth.getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                        .child("Registered Users")
                        .child(uid)
                        .child("Child");
                ref.removeEventListener(lockStatusListener);
            }
        }
    }
} 