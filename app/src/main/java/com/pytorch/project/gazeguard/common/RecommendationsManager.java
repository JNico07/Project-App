package com.pytorch.project.gazeguard.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.pytorch.demo.objectdetection.R;

import java.util.Arrays;
import java.util.List;

public class RecommendationsManager {
    
    private static final List<Recommendation> recommendations = Arrays.asList(
        new Recommendation(
            "Screen-Free Time Before Bed",
            "Turn off all screens a half hour to an hour before sleep to promote better sleep habits.",
            R.drawable.noscreen
        ),
        new Recommendation(
            "Reading Time",
            "Let's aim for an extra hour of reading next week to enhance cognitive development.",
            R.drawable.reading
        ),
        new Recommendation(
            "Non-Screen Activities",
            "Try non-screen activities such as outdoor play, reading, and family time.",
            R.drawable.outdoor
        ),
        new Recommendation(
            "Better Sleep Routine",
            "To improve sleep quality, limit screen time to 30 minutes before bed and incorporate a relaxing bedtime routine.",
            R.drawable.sleeptime
        ),
        new Recommendation(
            "Reduce Social Media Time",
            "Consider reducing social media time by 10 minutes tomorrow.",
            R.drawable.socialmedia
        )
    );

    public static void showRecommendationsDialog(Context context) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_recommendations, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recommendationsRecyclerView);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        RecommendationsAdapter adapter = new RecommendationsAdapter(recommendations);
        recyclerView.setAdapter(adapter);

        new MaterialAlertDialogBuilder(context)
            .setTitle("Recommendations")
            .setView(dialogView)
            .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
            .show();
    }

    public static class Recommendation {
        String title;
        String description;
        int iconResId;

        public Recommendation(String title, String description, int iconResId) {
            this.title = title;
            this.description = description;
            this.iconResId = iconResId;
        }
    }
} 