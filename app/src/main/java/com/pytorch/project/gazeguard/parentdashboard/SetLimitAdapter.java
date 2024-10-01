package com.pytorch.project.gazeguard.parentdashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.slider.Slider;
import com.google.firebase.database.FirebaseDatabase;
import com.pytorch.project.gazeguard.common.TooltipFormatter;

import org.pytorch.demo.objectdetection.R;

public class SetLimitAdapter extends FirebaseRecyclerAdapter<ParentModel, SetLimitAdapter.SetLimitViewHolder> {

    private String uid;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateLimitTask, updateUnlockTimeTask;
    TooltipFormatter tooltipFormatter = new TooltipFormatter();

    public SetLimitAdapter(@NonNull FirebaseRecyclerOptions<ParentModel> options, String uid) {
        super(options);
        this.uid = uid;
    }

    @Override
    protected void onBindViewHolder(@NonNull SetLimitViewHolder holder, int position, @NonNull ParentModel model) {
        // Clear existing listeners to avoid triggering events from recycled views
        holder.slider.clearOnChangeListeners();
        holder.timePicker.setOnTimeChangedListener(null);

        holder.userName.setText(model.getName());

        // Set the initial slider value based on data in the model
        holder.slider.setValue(model.getScreenTimeLimit());
        holder.screenTimeLimit.setText("Limit: " + model.getScreenTimeLimit() + " Hrs");

        // Set the initial time picker value based on the model
        String[] unlockTimeParts = model.getDeviceUnlockTime().split("[: ]");
        int hourOfDay = Integer.parseInt(unlockTimeParts[0]);
        int minute = Integer.parseInt(unlockTimeParts[1]);
        String amPm = unlockTimeParts[2];
        if ("PM".equals(amPm) && hourOfDay != 12) {
            hourOfDay += 12;
        } else if ("AM".equals(amPm) && hourOfDay == 12) {
            hourOfDay = 0;
        }
        holder.timePicker.setHour(hourOfDay);
        holder.timePicker.setMinute(minute);
        holder.deviceUnlockTime.setText("Unlock Time: " + model.getDeviceUnlockTime());


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

        // Time picker listener
        holder.timePicker.setOnTimeChangedListener((view, hourOfDaySelected, minuteSelected) -> {
            String formattedTime = formatTime(hourOfDaySelected, minuteSelected);
            holder.deviceUnlockTime.setText("Unlock Time: " + formattedTime);
            if (updateUnlockTimeTask != null) {
                handler.removeCallbacks(updateUnlockTimeTask);
            }
            updateUnlockTimeTask = () -> {
                // Save the updated unlock time to Firebase
                FirebaseDatabase.getInstance().getReference("Registered Users")
                        .child(uid)
                        .child("Child")
                        .child(getRef(position).getKey())
                        .child("deviceUnlockTime")
                        .setValue(formattedTime);
            };
            handler.postDelayed(updateUnlockTimeTask, 1300);
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_limit_slider, parent, false);
        return new SetLimitViewHolder(view);
    }

    static class SetLimitViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView screenTimeLimit;
        TextView deviceUnlockTime;
        Slider slider;
        TimePicker timePicker;
        ImageView questionMarkSetLimit;
        ImageView questionMarkSetUnlockTime;

        public SetLimitViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            screenTimeLimit = itemView.findViewById(R.id.screenTimeLimit);
            deviceUnlockTime = itemView.findViewById(R.id.deviceUnlockTime);
            slider = itemView.findViewById(R.id.slider);
            timePicker = itemView.findViewById(R.id.timePicker);
            questionMarkSetLimit = itemView.findViewById(R.id.questionMarkSetLimit);
            questionMarkSetUnlockTime = itemView.findViewById(R.id.questionMarkSetUnlockTime);
        }
    }

    private String formatTime(int hourOfDay, int minute) {
        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
        int hour = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
        String formattedMinute = (minute < 10) ? "0" + minute : String.valueOf(minute);
        return hour + ":" + formattedMinute + " " + amPm;
    }
}
