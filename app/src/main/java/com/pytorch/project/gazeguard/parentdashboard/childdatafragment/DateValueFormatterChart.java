package com.pytorch.project.gazeguard.parentdashboard.childdatafragment;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateValueFormatterChart extends ValueFormatter {
    private final List<String> dates;
    private final SimpleDateFormat dateFormat;

    public DateValueFormatterChart(List<String> dates) {
        this.dates = dates;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // Format as "October 15, 2024"
    }

    @Override
    public String getFormattedValue(float value) {
        int index = (int) value;
        if (index >= 0 && index < dates.size()) {
            try {
                // Parse each date from "yyyy-MM-dd" and format it as "MMMM dd, yyyy"
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates.get(index));
                return dateFormat.format(date);
            } catch (Exception e) {
                e.printStackTrace();
                return dates.get(index); // Return the original date string if parsing fails
            }
        } else {
            return "";
        }
    }
}
