package com.pytorch.project.gazeguard.parentdashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;


import org.pytorch.demo.objectdetection.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParentAdapter extends FirebaseRecyclerAdapter<ParentModel, ParentAdapter.myViewHolder> {

    public ParentAdapter(@NonNull FirebaseRecyclerOptions<ParentModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull myViewHolder myViewHolder, int i, @NonNull ParentModel parentModel) {
        myViewHolder.name.setText(parentModel.getName());
        myViewHolder.SreenTime.setText(parentModel.getScreenTime());

        Glide.with(myViewHolder.img.getContext())
                .load(parentModel.getSurl())
                .placeholder(R.drawable.person)
                .circleCrop()
                .error(R.drawable.person)
                .into(myViewHolder.img);
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.parent_db_item,parent,false);
        return new myViewHolder(view);
    }

    static class myViewHolder extends RecyclerView.ViewHolder {
        CircleImageView img;
        TextView name, SreenTime;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);

            img = (CircleImageView) itemView.findViewById(R.id.img1);
            name = (TextView) itemView.findViewById(R.id.userName);
            SreenTime = (TextView) itemView.findViewById(R.id.timerTextPDash);
        }
    }

}
