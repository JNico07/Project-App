package com.pytorch.project.gazeguard.parentdashboard.childdatafragment;

import static com.pytorch.project.gazeguard.common.RecommendationsManager.showRecommendationsDialog;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.angmarch.views.NiceSpinner;
import org.pytorch.demo.objectdetection.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pytorch.project.gazeguard.common.FirebaseManager;

public class ChildDataFragment extends Fragment {

    private static final String ARG_CHILD_NAME = "child_name";
    private static final String ARG_CHILD_DATA_LIST = "child_data_list";
    private List<Map<String, Object>> childDataList;

    public static ChildDataFragment newInstance(String childName, List<Map<String, Object>> childDataList) {
        ChildDataFragment fragment = new ChildDataFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_NAME, childName);
        args.putSerializable(ARG_CHILD_DATA_LIST, (java.io.Serializable) childDataList);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_child_data_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView childNameTextView = view.findViewById(R.id.childNameTextView);
        LinearLayout recordsContainer = view.findViewById(R.id.recordsContainer);
        LineChart screenTimeChart = view.findViewById(R.id.screenTimeChart);
        NiceSpinner yearSpinner = view.findViewById(R.id.yearSpinner);

        if (getArguments() != null) {
            String childName = getArguments().getString(ARG_CHILD_NAME);
            childDataList = (List<Map<String, Object>>) getArguments().getSerializable(ARG_CHILD_DATA_LIST);

            childNameTextView.setText(childName);

            if (childDataList != null) {
                // Set up unique years from data
                Set<String> uniqueYears = new HashSet<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // Sort the childDataList by date in descending order
                childDataList.sort((record1, record2) -> {
                    try {
                        Date date1 = dateFormat.parse((String) record1.get("date"));
                        Date date2 = dateFormat.parse((String) record2.get("date"));
                        assert date2 != null;
                        return date2.compareTo(date1); // Descending order
                    } catch (Exception e) {
                        Log.d("ChildDataFragment", "Error parsing date: " + e.getMessage());
                    }
                    return 0;
                });

                for (Map<String, Object> record : childDataList) {
                    String date = (String) record.get("date");
                    assert date != null;
                    String year = date.substring(0, 4); // Extract year
                    uniqueYears.add(year);
                }

                List<String> sortedYears = new ArrayList<>(uniqueYears);
                Collections.sort(sortedYears, Collections.reverseOrder());
                sortedYears.add(0, "All"); // Add "All" as the first option
                yearSpinner.attachDataSource(sortedYears);

                // Set up listener for year selection
                yearSpinner.setOnSpinnerItemSelectedListener((parent, view1, position, id) -> {
                    String selectedYear = sortedYears.get(position);
                    updateRecordsContainerForYear(recordsContainer, screenTimeChart, selectedYear);
                });

                // Initial display for "All" option
                updateRecordsContainerForYear(recordsContainer, screenTimeChart, "All");
            }
        }

        // Add click listener for the screen time info icon
        ImageView screenTimeInfoIcon = view.findViewById(R.id.screenTimeInfoIcon);
        screenTimeInfoIcon.setColorFilter(getResources().getColor(R.color.app_theme));
        screenTimeInfoIcon.setOnClickListener(v -> showTimeFormatInfo());
    }

    // Update recordsContainer based on the selected year
    private void updateRecordsContainerForYear(LinearLayout recordsContainer, LineChart screenTimeChart, String year) {
        recordsContainer.removeAllViews();

        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        // Get Firestore instance
        FirebaseFirestore firestore = FirebaseManager.getFirestore();
        String uid = FirebaseManager.getCurrentUser().getUid();
        String childName = getArguments().getString(ARG_CHILD_NAME);

        // Fetch app usage data
        firestore.collection("AppUsageRecords")
                .document(uid)
                .collection(childName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Modified: Use nested map to store aggregated app usage by date
                    Map<String, Map<String, Long>> aggregatedAppUsageByDate = new HashMap<>();

                    // Process and aggregate app usage records
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Date timestamp = document.getDate("timestamp");
                        if (timestamp != null) {
                            String date = inputDateFormat.format(timestamp);
                            @SuppressWarnings("unchecked")
                            Map<String, Long> appUsage = (Map<String, Long>) document.get("appUsage");

                            // Skip records with more than 25 apps
                            if (appUsage != null && appUsage.size() <= 25) {
                                // Get or create map for this date
                                Map<String, Long> dateAppUsage = aggregatedAppUsageByDate
                                    .computeIfAbsent(date, k -> new HashMap<>());

                                // Aggregate app usage times
                                for (Map.Entry<String, Long> app : appUsage.entrySet()) {
                                    String appName = app.getKey();
                                    Long usageTime = app.getValue();
                                    dateAppUsage.merge(appName, usageTime, Long::sum);
                                }
                            }
                        }
                    }

                    // Continue with existing screen time processing
                    Map<String, Float> aggregatedData = new HashMap<>();
                    for (Map<String, Object> record : childDataList) {
                        String date = (String) record.get("date");
                        if (year.equals("All") || (date != null && date.startsWith(year))) {
                            float screenTime = Float.parseFloat(String.valueOf(record.get("screenTime")));
                            aggregatedData.put(date, aggregatedData.getOrDefault(date, 0f) + screenTime);
                        }
                    }

                    // Sort and display records
                    List<Map.Entry<String, Float>> sortedAggregatedData = new ArrayList<>(aggregatedData.entrySet());
                    sortedAggregatedData.sort((entry1, entry2) -> {
                        try {
                            Date date1 = inputDateFormat.parse(entry1.getKey());
                            Date date2 = inputDateFormat.parse(entry2.getKey());
                            return date2.compareTo(date1);
                        } catch (Exception e) {
                            return 0;
                        }
                    });

                    int index = 0;
                    for (Map.Entry<String, Float> entry : sortedAggregatedData) {
                        String date = entry.getKey();
                        float totalScreenTime = entry.getValue();

                        View recordView = LayoutInflater.from(getContext()).inflate(R.layout.record_item, recordsContainer, false);
                        TextView dateTextView = recordView.findViewById(R.id.dateTextView);
                        TextView screenTimeTextView = recordView.findViewById(R.id.screenTimeTextView);
                        LinearLayout appUsageContainer = recordView.findViewById(R.id.appUsageContainer);
                        LinearLayout mainContainer = recordView.findViewById(R.id.mainContainer);
                        ImageView expandIndicator = recordView.findViewById(R.id.expandIndicator);

                        // Hide app usage container by default
                        appUsageContainer.setVisibility(View.GONE);
                        expandIndicator.setImageResource(R.drawable.ic_expand_more);
                        expandIndicator.setRotation(0);

                        try {
                            Date parsedDate = inputDateFormat.parse(date);
                            String formattedDate = outputDateFormat.format(parsedDate);
                            dateTextView.setText(formattedDate);
                        } catch (Exception e) {
                            dateTextView.setText(date);
                        }

                        screenTimeTextView.setText(formatScreenTime(totalScreenTime));

                        // Add click listener to the main container
                        mainContainer.setOnClickListener(v -> {
                            // Toggle visibility with animation
                            if (appUsageContainer.getVisibility() == View.VISIBLE) {
                                appUsageContainer.setVisibility(View.GONE);
                                // Animate the indicator
                                expandIndicator.animate().rotation(0).setDuration(200).start();
                            } else {
                                appUsageContainer.setVisibility(View.VISIBLE);
                                // Animate the indicator
                                expandIndicator.animate().rotation(180).setDuration(200).start();
                            }
                        });

                        // Modified app usage display section
                        Map<String, Long> dateAppUsage = aggregatedAppUsageByDate.get(date);
                        if (dateAppUsage != null && !dateAppUsage.isEmpty()) {
                            // Calculate total app usage time
                            long totalAppUsageTime = dateAppUsage.values().stream()
                                    .mapToLong(Long::longValue)
                                    .sum();

                            // Sort apps by usage time (descending)
                            List<Map.Entry<String, Long>> sortedApps = new ArrayList<>(dateAppUsage.entrySet());
                            sortedApps.sort((app1, app2) -> app2.getValue().compareTo(app1.getValue()));

                            // Add total apps used count
                            TextView totalAppsView = new TextView(getContext());
                            totalAppsView.setText(String.format("Apps used: %d", sortedApps.size()));
                            totalAppsView.setTextSize(14);
                            totalAppsView.setPadding(40, 10, 20, 10);
                            totalAppsView.setTypeface(null, Typeface.BOLD);
                            appUsageContainer.addView(totalAppsView);

                            // Add top apps section
                            for (int i = 0; i < Math.min(5, sortedApps.size()); i++) {
                                Map.Entry<String, Long> appEntry = sortedApps.get(i);
                                String appName = getAppNameFromPackage(appEntry.getKey());
                                
                                // Create app usage row layout
                                LinearLayout appRow = new LinearLayout(getContext());
                                appRow.setOrientation(LinearLayout.HORIZONTAL);
                                appRow.setPadding(40, 5, 20, 5);
                                
                                // App name with progress
                                LinearLayout appInfoLayout = new LinearLayout(getContext());
                                appInfoLayout.setOrientation(LinearLayout.VERTICAL);
                                appInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                                
                                TextView appNameView = new TextView(getContext());
                                appNameView.setText(appName);
                                appNameView.setTextSize(14);
                                
                                ProgressBar progressBar = new ProgressBar(getContext(), null, 
                                        android.R.attr.progressBarStyleHorizontal);
                                // Calculate percentage based on total app usage time
                                int percentage = (int) ((appEntry.getValue() * 100.0f) / totalAppUsageTime);
                                progressBar.setProgress(percentage);
                                progressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.app_theme)));
                                
                                appInfoLayout.addView(appNameView);
                                appInfoLayout.addView(progressBar);
                                
                                // Usage time
                                TextView usageTimeView = new TextView(getContext());
                                usageTimeView.setText(formatScreenTime(appEntry.getValue()));
                                usageTimeView.setTextSize(14);
                                
                                appRow.addView(appInfoLayout);
                                appRow.addView(usageTimeView);
                                
                                appUsageContainer.addView(appRow);
                            }

                            // Add "Show More" button if there are more apps
                            if (sortedApps.size() > 5) {
                                // Customize "Show More" button appearance and interaction
                                Button showMoreButton = new Button(getContext());
                                showMoreButton.setText("View All Apps");
                                showMoreButton.setTextSize(14);
                                showMoreButton.setAllCaps(false);
                                showMoreButton.setBackgroundResource(R.drawable.rounded_button); // Use a custom drawable for the button
                                showMoreButton.setTextColor(getResources().getColor(R.color.white));
                                showMoreButton.setPadding(30, 20, 30, 20);

                                // Set margins and center alignment
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                params.gravity = Gravity.CENTER_HORIZONTAL;
                                params.setMargins(20, 20, 20, 20);
                                showMoreButton.setLayoutParams(params);

                                // Add ripple effect on touch (if not using background with ripple already)
                                showMoreButton.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.ripple_effect, null));

                                // Set up a click listener with animation
                                showMoreButton.setOnClickListener(v -> {
                                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                                        v.animate().scaleX(1f).scaleY(1f).setDuration(100);
                                        showAllAppsDialog(sortedApps);
                                    }).start();
                                });

                                // Add to container
                                appUsageContainer.addView(showMoreButton);

                            }
                        }

                        recordsContainer.addView(recordView);

                        entries.add(new Entry(index++, totalScreenTime));
                        dates.add(date);
                    }

                    // Update chart
                    updateChart(screenTimeChart, entries, dates);
                });
    }

    private void updateChart(LineChart screenTimeChart, List<Entry> entries, List<String> dates) {
        // Update the chart data
        LineDataSet dataSet = new LineDataSet(entries, "Total Screen Time");
        dataSet.setColor(ColorTemplate.COLORFUL_COLORS[0]);
        dataSet.setValueTextColor(ColorTemplate.COLORFUL_COLORS[0]);

        dataSet.setValueFormatter(new ValueFormatterChart());
        LineData lineData = new LineData(dataSet);
        screenTimeChart.setData(lineData);

        // Configure chart X-axis with dates in ascending order
        XAxis xAxis = screenTimeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5, true);
        xAxis.setValueFormatter(new DateValueFormatterChart(dates));
        xAxis.setLabelRotationAngle(45f);
        xAxis.setDrawGridLines(false);

        // Y Axis
        screenTimeChart.getAxisLeft().setValueFormatter(new ValueFormatterChart());
        screenTimeChart.getAxisRight().setEnabled(false); // Optionally disable the right Y-axis if not needed

        screenTimeChart.invalidate(); // Refresh the chart
    }

    private String formatScreenTime(float screenTimeInSeconds) {
        int hours = (int) (screenTimeInSeconds / 3600);
        int minutes = (int) ((screenTimeInSeconds % 3600) / 60);
        int seconds = (int) (screenTimeInSeconds % 60);

        StringBuilder formattedTime = new StringBuilder();
        
        if (hours > 0) {
            formattedTime.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            formattedTime.append(minutes).append("m ");
        }
        formattedTime.append(seconds).append("s");

        return formattedTime.toString();
    }

    private String getAppNameFromPackage(String packageName) {
        // Common app package mappings
        Map<String, String> commonApps = new HashMap<String, String>() {{
            // Social Media
            put("com.whatsapp", "WhatsApp");
            put("com.facebook.katana", "Facebook");
            put("com.instagram.android", "Instagram");
            put("com.twitter.android", "Twitter");
            put("com.snapchat.android", "Snapchat");
            put("com.linkedin.android", "LinkedIn");
            put("com.ss.android.ugc.trill", "TikTok");
            put("com.zhiliaoapp.musically.go", "TikTok");

            // Google Apps
            put("com.google.android.youtube", "YouTube");
            put("com.google.android.gm", "Gmail");
            put("com.google.android.apps.maps", "Google Maps");
            put("com.android.chrome", "Chrome");
            put("com.google.android.apps.photos", "Google Photos");
            put("com.google.android.calendar", "Google Calendar");
            put("System launcher", "Home Screen");

            // Messaging & Communication
            put("com.facebook.messenger", "Messenger");
            put("com.facebook.orca", "Messenger");
            put("org.telegram.messenger", "Telegram");
            put("com.viber.voip", "Viber");
            put("com.skype.raider", "Skype");
            put("com.mydito", "DITO");

            // Entertainment
            put("com.spotify.music", "Spotify");
            put("com.netflix.mediaclient", "Netflix");
            put("tv.twitch.android.app", "Twitch");
            put("com.valvesoftware.android.steam.community", "Steam");
            put("com.lazada.android", "Lazada");
            put("com.nbaimd.gametime.nba2011", "NBA");

            // Gaming
            put("com.supercell.clashofclans", "Clash of Clans");
            put("com.mojang.minecraftpe", "Minecraft");
            put("com.kiloo.subwaysurf", "Subway Surfers");

            // Productivity
            put("com.microsoft.office.word", "Word");
            put("com.microsoft.office.excel", "Excel");
            put("com.microsoft.office.powerpoint", "PowerPoint");
            put("com.microsoft.outlook", "Outlook");
            put("com.google.android.apps.docs", "Google Docs");
            put("com.github.android", "Github");

            put("com.bybit.app", "Bybit");
        }};

        // Check if the package is in our common apps map
        if (commonApps.containsKey(packageName)) {
            return commonApps.get(packageName);
        }

        // Try to get from PackageManager first
        try {
            PackageManager packageManager = requireContext().getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            // If package not found, extract and format the last part of the package name
            String[] parts = packageName.split("\\.");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                // Capitalize first letter
                return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
            }
            return packageName; // Fallback to full package name if splitting fails
        }
    }

    private void showAllAppsDialog(List<Map.Entry<String, Long>> apps) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("App Usage Details");

        // Create ScrollView for the content
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout contentLayout = new LinearLayout(requireContext());
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(20, 20, 20, 20);

        // Create header layout with summary and info icon
        LinearLayout headerLayout = new LinearLayout(requireContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        // Add summary section
        TextView summaryText = new TextView(requireContext());
        summaryText.setText(String.format("Total Apps Used: %d", apps.size()));
        summaryText.setTextSize(16);
        summaryText.setTypeface(null, Typeface.BOLD);
        summaryText.setPadding(0, 0, 10, 0);
        headerLayout.addView(summaryText);

        // Add info icon
        ImageView infoIcon = new ImageView(requireContext());
        infoIcon.setImageResource(R.drawable.ic_info);
        infoIcon.setColorFilter(getResources().getColor(R.color.app_theme));
        int iconSize = (int) (summaryText.getTextSize() * 1.2); // Make icon slightly larger than text
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        infoIcon.setLayoutParams(iconParams);
        infoIcon.setOnClickListener(v -> showTimeFormatInfo());
        headerLayout.addView(infoIcon);

        contentLayout.addView(headerLayout);
        
        // Add padding below header
        View headerPadding = new View(requireContext());
        LinearLayout.LayoutParams paddingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 20);
        headerPadding.setLayoutParams(paddingParams);
        contentLayout.addView(headerPadding);

        // Calculate total app usage time
        long totalAppUsageTime = apps.stream()
                .mapToLong(Map.Entry::getValue)
                .sum();

        // Sort apps by usage time
        List<Map.Entry<String, Long>> sortedApps = new ArrayList<>(apps);
        sortedApps.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Add each app's details
        for (Map.Entry<String, Long> appEntry : sortedApps) {
            // Create main row container
            LinearLayout appRow = new LinearLayout(requireContext());
            appRow.setOrientation(LinearLayout.HORIZONTAL);
            appRow.setPadding(30, 20, 30, 10);
            appRow.setGravity(Gravity.CENTER_VERTICAL);

            // Left side: App name and progress bar
            LinearLayout leftSection = new LinearLayout(requireContext());
            leftSection.setOrientation(LinearLayout.VERTICAL);
            leftSection.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            // App name
            TextView appNameView = new TextView(requireContext());
            String appName = getAppNameFromPackage(appEntry.getKey());
            appNameView.setText(appName);
            appNameView.setTextSize(14);
            appNameView.setTypeface(null, Typeface.BOLD);
            leftSection.addView(appNameView);

            // Progress bar
            int percentage = (int) ((appEntry.getValue() * 100.0f) / totalAppUsageTime);
            ProgressBar progressBar = new ProgressBar(requireContext(), null, 
                    android.R.attr.progressBarStyleHorizontal);
            progressBar.setProgress(percentage);
            progressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.app_theme)));
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 8);
            progressParams.topMargin = 5;
            progressBar.setLayoutParams(progressParams);
            leftSection.addView(progressBar);

            appRow.addView(leftSection);

            // Right side: Usage time and percentage
            LinearLayout rightSection = new LinearLayout(requireContext());
            rightSection.setOrientation(LinearLayout.VERTICAL);
            rightSection.setGravity(Gravity.END);
            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rightParams.setMarginStart(20); // Increased margin for better spacing
            rightSection.setLayoutParams(rightParams);

            // Time used
            TextView timeView = new TextView(requireContext());
            timeView.setText(formatScreenTime(appEntry.getValue()));
            timeView.setTextSize(13);
            timeView.setGravity(Gravity.END);
            rightSection.addView(timeView);

            // Percentage
            TextView percentageView = new TextView(requireContext());
            percentageView.setText(String.format("%d%%", percentage));
            percentageView.setTextSize(12);
            percentageView.setTextColor(Color.GRAY);
            percentageView.setGravity(Gravity.END);
            rightSection.addView(percentageView);

            appRow.addView(rightSection);

            contentLayout.addView(appRow);

            // Add separator
            View separator = new View(requireContext());
            separator.setBackgroundColor(Color.LTGRAY);
            LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            contentLayout.addView(separator);
        }

        scrollView.addView(contentLayout);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();

        // Style the dialog buttons
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(getResources().getColor(R.color.app_theme));
    }

    private void showTimeFormatInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Time Format Guide");
        
        // Create the message with formatted text
        SpannableStringBuilder message = new SpannableStringBuilder();
        
        // Add h (hours)
        message.append("h = ");
        int hourStart = message.length();
        message.append("hour/s");
        message.setSpan(new StyleSpan(Typeface.BOLD), hourStart, message.length(), 
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.append("\n");
        
        // Add m (minutes)
        message.append("m = ");
        int minStart = message.length();
        message.append("minute/s");
        message.setSpan(new StyleSpan(Typeface.BOLD), minStart, message.length(), 
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.append("\n");
        
        // Add s (seconds)
        message.append("s = ");
        int secStart = message.length();
        message.append("second/s");
        message.setSpan(new StyleSpan(Typeface.BOLD), secStart, message.length(), 
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.append("\n\n");
        
        // Add example
        message.append("Example: ");
        message.append("1h 32m 45s = ");
        int exampleStart = message.length();
        message.append("1 hour 32 minutes 45 seconds");
        message.setSpan(new StyleSpan(Typeface.BOLD), exampleStart, message.length(), 
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.setMessage(message);
        builder.setPositiveButton("Got it", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Style the dialog button
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(getResources().getColor(R.color.app_theme));
    }

}
