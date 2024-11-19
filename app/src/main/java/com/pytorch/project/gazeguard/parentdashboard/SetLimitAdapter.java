package com.pytorch.project.gazeguard.parentdashboard;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pytorch.project.gazeguard.common.TooltipFormatter;

import org.pytorch.demo.objectdetection.R;

public class SetLimitAdapter extends FirebaseRecyclerAdapter<ParentModel, SetLimitAdapter.SetLimitViewHolder> {

    private String uid;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateLimitTask;
    TooltipFormatter tooltipFormatter = new TooltipFormatter();
    private int hour, minute;
    private DatabaseReference lockStatusRef, unlockTimeRef;
    private static final String CHANNEL_ID = "device_lock_channel";
    private static final int NOTIFICATION_ID = 1001;
    private boolean isLockedLocalCheck = false;

    public SetLimitAdapter(@NonNull FirebaseRecyclerOptions<ParentModel> options, String uid) {
        super(options);
        this.uid = uid;
    }

    @Override
    protected void onBindViewHolder(@NonNull SetLimitViewHolder holder, int position, @NonNull ParentModel model) {
        // Clear existing listeners to avoid triggering events from recycled views
        holder.itemView.setOnClickListener(null);

        holder.userName.setText(model.getName());

        // Set the initial slider value based on data in the model
        holder.screenTimeLimitTextView.setText("Limit: " + formatScreenTimeLimit(model.getScreenTimeLimit()));
        holder.deviceUnlockTimeTextView.setText("Unlock Time: " + model.getDeviceUnlockTime());

        // Add listener for lock status
        lockStatusRef = FirebaseDatabase.getInstance().getReference("Registered Users")
                .child(uid)
                .child("Child")
                .child(getRef(position).getKey())
                .child("isDeviceLocked");
        lockStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isLocked = snapshot.getValue(Boolean.class);
                if (isLocked != null && isLocked) {
                    holder.lockStatusIcon.setVisibility(View.VISIBLE);

                    isLockedLocalCheck = true;

                    // Show notification when device is locked
                    showDeviceLockNotification(holder.itemView.getContext(), model.getName());
                } else {
                    holder.lockStatusIcon.setVisibility(View.GONE);
                    isLockedLocalCheck = false;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SetLimitAdapter", "Error reading lock status", error.toException());
            }
        });

        unlockTimeRef = FirebaseDatabase.getInstance().getReference("Registered Users")
                .child(uid)
                .child("Child")
                .child(getRef(position).getKey())
                .child("deviceUnlockTime");
        unlockTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String unlockTime = snapshot.getValue(String.class);
                if (unlockTime == null) {
                    // Set default value if null
                    unlockTimeRef.setValue("5:00 AM");
                    model.setDeviceUnlockTime("5:00 AM");
                    holder.deviceUnlockTimeTextView.setText("Unlock Time: 5:00 AM");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SetLimitAdapter", "Error reading unlock time", error.toException());
            }
        });

        // SET LIMIT TIME
        holder.timeLimitPickerButton.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTitleText("Select Unlock Time")
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .build();

            picker.addOnPositiveButtonClickListener(view -> {
                hour = picker.getHour();
                minute = picker.getMinute();

                // Convert hours and minutes to total seconds
                int totalSeconds = (hour * 3600) + (minute * 60);

                holder.screenTimeLimitTextView.setText("Limit: " + hour + " Hrs " + minute + " Min");

                // Set the total seconds as the screen time limit
                model.setScreenTimeLimit(totalSeconds);

                // Save totalSeconds in Firebase (optional, if needed)
                FirebaseDatabase.getInstance().getReference("Registered Users")
                        .child(uid)
                        .child("Child")
                        .child(getRef(position).getKey())
                        .child("screenTimeLimit")
                        .setValue(totalSeconds);
            });
            picker.show(((AppCompatActivity) holder.itemView.getContext()).getSupportFragmentManager(), "timePicker");
        });
        // SET UNLOCK TIME
        holder.timeUnlockPickerButton.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTitleText("Select Unlock Time")
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .build();

            picker.addOnPositiveButtonClickListener(view -> {
                int hour = picker.getHour();
                int minute = picker.getMinute();
                String formattedTime = formatTime(hour, minute);
                holder.deviceUnlockTimeTextView.setText("Unlock Time: " + formattedTime);
                model.setDeviceUnlockTime(formattedTime); // Update the model with new unlock time

                // Save the updated unlock time to Firebase
                FirebaseDatabase.getInstance().getReference("Registered Users")
                        .child(uid)
                        .child("Child")
                        .child(getRef(position).getKey())
                        .child("deviceUnlockTime")
                        .setValue(formattedTime);
            });
            picker.show(((AppCompatActivity) holder.itemView.getContext()).getSupportFragmentManager(), "timePicker");
        });

        String deviceUnlockTime = model.getDeviceUnlockTime();
        if (deviceUnlockTime != null) {
            holder.deviceUnlockTimeTextView.setText("Unlock Time: " + deviceUnlockTime);
        } else {
            holder.deviceUnlockTimeTextView.setText("Unlock Time: Not Set");
        }


        // Unlock Button
        holder.isUnlockDeviceButton.setOnClickListener(v -> {
            // Check if the device is locked
            lockStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean isLocked = snapshot.getValue(Boolean.class);
                    if (isLocked != null && isLocked || isLockedLocalCheck) {
                        // Show the Material Dialog if the device is locked
                        new MaterialAlertDialogBuilder(holder.itemView.getContext())
                                .setTitle("Unlock Device")
                                .setMessage("Are you sure you want to unlock the device?")
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    // Update Firebase to trigger device unlock
                                    FirebaseDatabase.getInstance().getReference("Registered Users")
                                            .child(uid)
                                            .child("Child")
                                            .child(getRef(holder.getAdapterPosition()).getKey())
                                            .child("isUnlockDeviceNow")
                                            .setValue(true)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(holder.itemView.getContext(), model.getName() +
                                                        "'s Device is now Unlocked", Toast.LENGTH_SHORT).show();

                                                // Reset the value after a short delay
                                                handler.postDelayed(() -> {
                                                    FirebaseDatabase.getInstance().getReference("Registered Users")
                                                            .child(uid)
                                                            .child("Child")
                                                            .child(getRef(holder.getAdapterPosition()).getKey())
                                                            .child("isUnlockDeviceNow")
                                                            .setValue(false);
                                                }, 2000); // Reset after 2 seconds
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(holder.itemView.getContext(),
                                                            "Failed to unlock device", Toast.LENGTH_SHORT).show()
                                            );
                                })
                                .show();
                    } else {
                        // Show a Toast message if the device is not locked
                        Toast.makeText(holder.itemView.getContext(), model.getName() +
                                "'s device is currently unlocked", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("SetLimitAdapter", "Error reading lock status", error.toException());
                }
            });
        });


        // Tooltips
        holder.questionMarkSetLimit.setOnClickListener(v ->
                tooltipFormatter.setToolTip(holder.itemView.getContext(), v, "Here you can set Screen Time Limit and it Automatically Lock user's device"));
        holder.questionMarkSetUnlockTime.setOnClickListener(v ->
                tooltipFormatter.setToolTip(holder.itemView.getContext(), v, "Here you can set schedule when to remove auto lock to user's device"));

    }

    @NonNull
    @Override
    public SetLimitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_limit_lock_unlock, parent, false);
        return new SetLimitViewHolder(view);
    }

    static class SetLimitViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView screenTimeLimitTextView;
        TextView deviceUnlockTimeTextView;
        Button timeUnlockPickerButton, timeLimitPickerButton,isUnlockDeviceButton;
        ImageView questionMarkSetLimit;
        ImageView questionMarkSetUnlockTime;
        ImageView lockStatusIcon;

        public SetLimitViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            screenTimeLimitTextView = itemView.findViewById(R.id.screenTimeLimitTextView);
            deviceUnlockTimeTextView = itemView.findViewById(R.id.deviceUnlockTimeTextView);
            timeUnlockPickerButton = itemView.findViewById(R.id.timeUnlockPickerButton);
            timeLimitPickerButton = itemView.findViewById(R.id.timeLimitPickerButton);
            questionMarkSetLimit = itemView.findViewById(R.id.questionMarkSetLimit);
            questionMarkSetUnlockTime = itemView.findViewById(R.id.questionMarkSetUnlockTime);
            isUnlockDeviceButton = itemView.findViewById(R.id.unlockDeviceButton);
            lockStatusIcon = itemView.findViewById(R.id.lockStatusIcon);
        }
    }

    private String formatTime(int hourOfDay, int minute) {
        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
        int hour = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
        String formattedMinute = (minute < 10) ? "0" + minute : String.valueOf(minute);
        return hour + ":" + formattedMinute + " " + amPm;
    }

    // Method to convert total seconds to hours and minutes
    private String formatScreenTimeLimit(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        return hours + " Hrs " + minutes + " Min";
    }

    private void showDeviceLockNotification(Context context, String userName) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "Device Lock Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Notifications for when a child's device is locked");
        notificationManager.createNotificationChannel(channel);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_important)
                .setContentTitle("Device Locked")
                .setContentText(userName + " reach Screen Time Limit, device has been locked")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
