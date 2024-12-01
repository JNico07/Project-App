package com.pytorch.project.gazeguard.parentdashboard.childdatafragment;

import static com.pytorch.project.gazeguard.common.RecommendationsManager.showRecommendationsDialog;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

                            if (appUsage != null) {
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

                        // Hide app usage container by default
                        appUsageContainer.setVisibility(View.GONE);

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
                            } else {
                                appUsageContainer.setVisibility(View.VISIBLE);
                            }
                        });

                        // Add aggregated app usage details if available
                        Map<String, Long> dateAppUsage = aggregatedAppUsageByDate.get(date);
                        if (dateAppUsage != null && !dateAppUsage.isEmpty()) {
                            // Sort apps by usage time (descending)
                            List<Map.Entry<String, Long>> sortedApps = new ArrayList<>(dateAppUsage.entrySet());
                            sortedApps.sort((app1, app2) -> app2.getValue().compareTo(app1.getValue()));

                            for (Map.Entry<String, Long> appEntry : sortedApps) {
                                String appName = getAppNameFromPackage(appEntry.getKey()); // Convert package name to app name
                                TextView appUsageView = new TextView(getContext());
                                appUsageView.setText(String.format("%s: %s",
                                        appName,
                                        formatScreenTime(appEntry.getValue())));
                                appUsageView.setTextSize(14);
                                appUsageView.setPadding(40, 5, 20, 5);
                                appUsageContainer.addView(appUsageView);
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

        if (screenTimeInSeconds < 60) {
            return String.format("%d seconds", seconds);
        } else if (screenTimeInSeconds < 3600) {
            return String.format("%d minutes", minutes);
        } else {
            return String.format("%d hours", hours);
        }
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

}
