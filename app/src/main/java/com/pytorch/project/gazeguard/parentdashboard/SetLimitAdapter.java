package com.pytorch.project.gazeguard.parentdashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.slider.Slider;
import com.google.firebase.database.FirebaseDatabase;

import org.pytorch.demo.objectdetection.R;

public class SetLimitAdapter extends FirebaseRecyclerAdapter<ParentModel, SetLimitAdapter.SetLimitViewHolder> {

    private String uid;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTask;

    public SetLimitAdapter(@NonNull FirebaseRecyclerOptions<ParentModel> options, String uid) {
        super(options);
        this.uid = uid;
    }

    @Override
    protected void onBindViewHolder(@NonNull SetLimitViewHolder holder, int position, @NonNull ParentModel model) {
        holder.userName.setText(model.getName());

        // Set the initial slider value based on data in the model
        holder.slider.setValue(model.getLimitValue());

        // Set the initial TextView value based on data in the model
        holder.setLimitValue.setText("Limit: " + model.getLimitValue() + " Hrs");

        // Remove any existing listener to avoid triggering events from recycled views
        holder.slider.clearOnChangeListeners();

        // listener to capture the value selected on the slider
        holder.slider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int setLimit = (int) value;

                // Update the model with the new limit
                model.setLimitValue(setLimit);

                // Update the displayed limit in the TextView
                holder.setLimitValue.setText("Limit: " + setLimit + " Hrs");

                // Remove any pending updates
                if (updateTask != null) {
                    handler.removeCallbacks(updateTask);
                }

                // Create a new update task
                updateTask = () -> {
                    // Save the updated limit value to Firebase
                    FirebaseDatabase.getInstance().getReference("Registered Users")
                            .child(uid)
                            .child("Child")
                            .child(getRef(position).getKey())
                            .child("limitValue")
                            .setValue(setLimit);
                };

                // Post the update task to run after a delay
                handler.postDelayed(updateTask, 1000);
            }
        });

        // Set click listener to handle accessibility
        holder.slider.setOnClickListener(v -> {
            v.performClick(); // Call performClick on the Slider view itself
            // Handle the click action if needed
        });
    }

    @NonNull
    @Override
    public SetLimitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_limit_slider, parent, false);
        return new SetLimitViewHolder(view);
    }

    static class SetLimitViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView setLimitValue;
        Slider slider;

        public SetLimitViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            setLimitValue = itemView.findViewById(R.id.setLimitValue);
            slider = itemView.findViewById(R.id.slider);
        }
    }
}
