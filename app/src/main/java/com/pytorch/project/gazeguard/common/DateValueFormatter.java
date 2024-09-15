package com.pytorch.project.gazeguard.common;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.List;

// custom ValueFormatter to format the XAxis labels as date strings
public class DateValueFormatter extends ValueFormatter {
    private final List<String> dates;

    public DateValueFormatter(List<String> dates) {
        this.dates = dates;
    }

    @Override
    public String getFormattedValue(float value) {
        int index = (int) value;
        if (index >= 0 && index < dates.size()) {
            return dates.get(index); // Return the date string for the index
        } else {
            return "";
        }
    }
}
