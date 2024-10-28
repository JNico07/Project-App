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

import org.angmarch.views.NiceSpinner;
import org.pytorch.demo.objectdetection.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        recordsContainer.removeAllViews(); // Clear previous records

        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        // Filter data for the selected year
        List<Map<String, Object>> filteredData = new ArrayList<>();
        for (Map<String, Object> record : childDataList) {
            String date = (String) record.get("date");
            if (year.equals("All") || Objects.requireNonNull(date).startsWith(year)) {
                filteredData.add(record);
            }
        }

        // Sort filteredData in descending order for recordsContainer
        filteredData.sort((record1, record2) -> {
            try {
                Date date1 = inputDateFormat.parse((String) record1.get("date"));
                Date date2 = inputDateFormat.parse((String) record2.get("date"));
                return date2.compareTo(date1); // Descending order
            } catch (Exception e) {
                Log.d("ChildDataFragment", "Error parsing date: " + e.getMessage());
                return 0;
            }
        });

        // Populate recordsContainer in descending order
        for (Map<String, Object> record : filteredData) {
            String date = (String) record.get("date");
            float screenTime = Float.parseFloat(String.valueOf(record.get("screenTime")));

            // Inflate and populate the record views
            View recordView = LayoutInflater.from(getContext()).inflate(R.layout.record_item, recordsContainer, false);
            TextView dateTextView = recordView.findViewById(R.id.dateTextView);
            TextView screenTimeTextView = recordView.findViewById(R.id.screenTimeTextView);

            try {
                Date parsedDate = inputDateFormat.parse(date);
                String formattedDate = outputDateFormat.format(parsedDate);
                dateTextView.setText(formattedDate);
            } catch (Exception e) {
                Log.d("ChildDataFragment", "Error formatting date: " + e.getMessage());
                dateTextView.setText(date); // Fallback to original date if parsing fails
            }

            screenTimeTextView.setText(formatScreenTime(screenTime));
            recordsContainer.addView(recordView);
        }

        // Sort filteredData in ascending order for screenTimeChart
        filteredData.sort((record1, record2) -> {
            try {
                Date date1 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse((String) record1.get("date"));
                Date date2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse((String) record2.get("date"));
                assert date1 != null;
                return date1.compareTo(date2); // Ascending order
            } catch (Exception e) {
                Log.d("ChildDataFragment", "Error parsing date: " + e.getMessage());
                return 0;
            }
        });

        int index = 0;
        for (Map<String, Object> record : filteredData) {
            String date = (String) record.get("date");
            float screenTime = Float.parseFloat(String.valueOf(record.get("screenTime")));

            // Add data entries (index, screenTime) for chart
            entries.add(new Entry(index++, screenTime));
            dates.add(date);
        }

        // Update the chart data
        LineDataSet dataSet = new LineDataSet(entries, "Screen Time");
        dataSet.setColor(ColorTemplate.COLORFUL_COLORS[0]);
        dataSet.setValueTextColor(ColorTemplate.COLORFUL_COLORS[0]);

        // Apply the formatter to format data point values
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
            return String.format("%d hours %d min", hours, minutes);
        }
    }
}
