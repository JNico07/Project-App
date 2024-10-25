package com.pytorch.project.gazeguard.parentdashboard.childdatafragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.pytorch.project.gazeguard.common.DateValueFormatter;

import org.pytorch.demo.objectdetection.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class ChildDataFragment extends Fragment {

    private static final String ARG_CHILD_NAME = "child_name";
    private static final String ARG_CHILD_DATA_LIST = "child_data_list";

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

        if (getArguments() != null) {
            String childName = getArguments().getString(ARG_CHILD_NAME);
            List<Map<String, Object>> childDataList = (List<Map<String, Object>>) getArguments().getSerializable(ARG_CHILD_DATA_LIST);

            childNameTextView.setText(childName);

            if (childDataList != null) {
                // Sort the childDataList by date in order
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                childDataList.sort(new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> record1, Map<String, Object> record2) {
                        try {
                            Date date1 = dateFormat.parse((String) record1.get("date"));
                            Date date2 = dateFormat.parse((String) record2.get("date"));
                            assert date2 != null;
//                            return date1.compareTo(date2); // Ascending order
                            return date2.compareTo(date1); // Descending  order
                        } catch (Exception e) {
                            Log.d("ChildDataFragment", "Error parsing date: " + e.getMessage());
                        }
                        return 0;
                    }
                });

                List<Entry> entries = new ArrayList<>();
                List<String> dates = new ArrayList<>(); // List to store date strings

                for (int i = 0; i < childDataList.size(); i++) {
                    Map<String, Object> record = childDataList.get(i);
                    String date = (String) record.get("date");
                    float screenTime = Float.parseFloat(String.valueOf(record.get("screenTime")));

                    // Add the date to the dates list
                    dates.add(date);

                    // Add data entries (index, screenTime)
                    entries.add(new Entry(i, screenTime));

                    // Inflate and populate the record views
                    View recordView = LayoutInflater.from(getContext()).inflate(R.layout.record_item, recordsContainer, false);

                    TextView dateTextView = recordView.findViewById(R.id.dateTextView);
                    TextView screenTimeTextView = recordView.findViewById(R.id.screenTimeTextView);

                    // Set the date and formatted screen time
                    dateTextView.setText("Date: " + date);
                    screenTimeTextView.setText("Screen Time: " + formatScreenTime(screenTime));

                    // Add the record view to the container
                    recordsContainer.addView(recordView);
                }

                // Create a LineDataSet and LineData
                LineDataSet dataSet = new LineDataSet(entries, "Screen Time");
                dataSet.setColor(ColorTemplate.COLORFUL_COLORS[0]);
                dataSet.setValueTextColor(ColorTemplate.COLORFUL_COLORS[0]);

                LineData lineData = new LineData(dataSet);
                screenTimeChart.setData(lineData);

                // Configure XAxis
                XAxis xAxis = screenTimeChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Better visibility at the bottom
                xAxis.setGranularity(1f); // Allow intervals of 1 between values
                xAxis.setLabelCount(5, true); // Set a smaller label count, adjust as needed
                xAxis.setValueFormatter(new DateValueFormatter(dates)); // Set custom date formatter

                // Rotate the labels for better visibility
                xAxis.setLabelRotationAngle(45f); // Rotate the labels by 45 degrees

                // Avoid drawing grid lines to make the chart cleaner
                xAxis.setDrawGridLines(false);

                // Refresh the chart
                screenTimeChart.invalidate();
            }
        }
    }

    // Helper method to format screen time in "hh:mm:ss" format
    private String formatScreenTime(float screenTimeInSeconds) {
        int hours = (int) (screenTimeInSeconds / 3600);
        int minutes = (int) ((screenTimeInSeconds % 3600) / 60);
        int seconds = (int) (screenTimeInSeconds % 60);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}

