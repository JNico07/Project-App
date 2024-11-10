package com.pytorch.project.gazeguard.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.pytorch.demo.objectdetection.R;

import java.util.List;

public class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.ViewHolder> {
    private final List<RecommendationsManager.Recommendation> recommendations;

    public RecommendationsAdapter(List<RecommendationsManager.Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendationsManager.Recommendation recommendation = recommendations.get(position);
        holder.titleText.setText(recommendation.title);
        holder.descriptionText.setText(recommendation.description);
        holder.iconImage.setImageResource(recommendation.iconResId);
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImage;
        TextView titleText;
        TextView descriptionText;

        ViewHolder(View view) {
            super(view);
            iconImage = view.findViewById(R.id.recommendationIcon);
            titleText = view.findViewById(R.id.recommendationTitle);
            descriptionText = view.findViewById(R.id.recommendationDescription);
        }
    }
} 