package com.pytorch.project.gazeguard.parentdashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.slider.Slider;

import org.pytorch.demo.objectdetection.R;

public class SetLimitAdapter extends FirebaseRecyclerAdapter<ParentModel, SetLimitAdapter.SetLimitViewHolder> {

    public SetLimitAdapter(@NonNull FirebaseRecyclerOptions<ParentModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull SetLimitViewHolder holder, int position, @NonNull ParentModel model) {
        holder.userName.setText(model.getName());
        // You can set the slider value here if needed
    }

    @NonNull
    @Override
    public SetLimitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_limit_slider, parent, false);
        return new SetLimitViewHolder(view);
    }

    static class SetLimitViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        Slider slider;

        public SetLimitViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            slider = itemView.findViewById(R.id.slider);
        }
    }
}