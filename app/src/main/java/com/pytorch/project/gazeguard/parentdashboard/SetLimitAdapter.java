package com.pytorch.project.gazeguard.parentdashboard;

import android.content.DialogInterface;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
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
    private Runnable updateLimitTask, updateUnlockTimeTask;
    TooltipFormatter tooltipFormatter = new TooltipFormatter();
    private boolean isUnlockDeviceNow, checkIfIsUnlockDeviceNow;

    public SetLimitAdapter(@NonNull FirebaseRecyclerOptions<ParentModel> options, String uid) {
        super(options);
        this.uid = uid;
    }

    @Override
    protected void onBindViewHolder(@NonNull SetLimitViewHolder holder, int position, @NonNull ParentModel model) {
        // Clear existing listeners to avoid triggering events from recycled views
        holder.slider.clearOnChangeListeners();
        holder.userName.setText(model.getName());

        // Set the initial slider value based on data in the model
        holder.slider.setValue(model.getScreenTimeLimit());
        holder.screenTimeLimit.setText("Limit: " + model.getScreenTimeLimit() + " Hrs");

        // Time picker button click listener
        holder.timePickerButton.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTitleText("Select Unlock Time")
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .build();

            picker.addOnPositiveButtonClickListener(view -> {
                int hour = picker.getHour();
                int minute = picker.getMinute();
                String formattedTime = formatTime(hour, minute);
                holder.deviceUnlockTime.setText("Unlock Time: " + formattedTime);
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
            holder.deviceUnlockTime.setText("Unlock Time: " + deviceUnlockTime);
        } else {
            holder.deviceUnlockTime.setText("Unlock Time: Not Set");
        }

        // Slider listener
        holder.slider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int setLimit = (int) value;
                holder.screenTimeLimit.setText("Limit: " + setLimit + " Hrs");
                model.setScreenTimeLimit(setLimit); // Update model with new limit
                if (updateLimitTask != null) {
                    handler.removeCallbacks(updateLimitTask);
                }
                updateLimitTask = () -> {
                    // Save the updated limit value to Firebase
                    FirebaseDatabase.getInstance().getReference("Registered Users")
                            .child(uid)
                            .child("Child")
                            .child(getRef(position).getKey())
                            .child("screenTimeLimit")
                            .setValue(setLimit);
                };
                handler.postDelayed(updateLimitTask, 1300);

            }
        });

        // Unlock Button
        holder.isUnlockDeviceButton.setOnClickListener(v -> {
            // Check if device is currently unlocked
            DatabaseReference unlockRef = FirebaseDatabase.getInstance().getReference("Registered Users")
                    .child(uid)
                    .child("Child")
                    .child(getRef(position).getKey())
                    .child("isUnlockDeviceNow");
            unlockRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    checkIfIsUnlockDeviceNow = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("SetLimitAdapter", "Failed to read isUnlockDeviceNow value.", error.toException());
                }
            });
            if (checkIfIsUnlockDeviceNow) {
                Toast.makeText(holder.itemView.getContext(), "Device is currently Unlocked", Toast.LENGTH_SHORT).show();
            } else {
                // Show confirmation dialog
                new MaterialAlertDialogBuilder(holder.itemView.getContext())
                        .setTitle("Unlock Device")
                        .setMessage("Are you sure you want to unlock the device?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Yes", (dialog, which) -> {
                            isUnlockDeviceNow = true;
                            // Save the updated limit value to Firebase
                            FirebaseDatabase.getInstance().getReference("Registered Users")
                                    .child(uid)
                                    .child("Child")
                                    .child(getRef(position).getKey())
                                    .child("isUnlockDeviceNow")
                                    .setValue(isUnlockDeviceNow);
                            Toast.makeText(holder.itemView.getContext(), "Unlock Device Now", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            }
        });

        // Tooltips
        holder.questionMarkSetLimit.setOnClickListener(v ->
                tooltipFormatter.setToolTip(holder.itemView.getContext(), v, "Here you can set Screen Time Limit and it Automatically Lock user's device"));
        holder.questionMarkSetUnlockTime.setOnClickListener(v ->
                tooltipFormatter.setToolTip(holder.itemView.getContext(), v, "Here you can set schedule when to remove auto lock to user's device"));

        // Accessibility
        holder.slider.setOnClickListener(View::performClick);
    }

    @NonNull
    @Override
    public SetLimitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_limit_lock_unlock, parent, false);
        return new SetLimitViewHolder(view);
    }

    static class SetLimitViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView screenTimeLimit;
        TextView deviceUnlockTime;
        Slider slider;
        Button timePickerButton, isUnlockDeviceButton;
        ImageView questionMarkSetLimit;
        ImageView questionMarkSetUnlockTime;

        public SetLimitViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            screenTimeLimit = itemView.findViewById(R.id.screenTimeLimit);
            deviceUnlockTime = itemView.findViewById(R.id.deviceUnlockTime);
            slider = itemView.findViewById(R.id.slider);
            timePickerButton = itemView.findViewById(R.id.timePickerButton);
            questionMarkSetLimit = itemView.findViewById(R.id.questionMarkSetLimit);
            questionMarkSetUnlockTime = itemView.findViewById(R.id.questionMarkSetUnlockTime);
            isUnlockDeviceButton = itemView.findViewById(R.id.unlockDeviceButton);
        }
    }

    private String formatTime(int hourOfDay, int minute) {
        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
        int hour = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
        String formattedMinute = (minute < 10) ? "0" + minute : String.valueOf(minute);
        return hour + ":" + formattedMinute + " " + amPm;
    }
}
