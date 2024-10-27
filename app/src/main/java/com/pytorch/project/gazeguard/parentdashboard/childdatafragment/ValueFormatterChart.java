package com.pytorch.project.gazeguard.parentdashboard.childdatafragment;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class ValueFormatterChart extends ValueFormatter {
    @Override
    public String getFormattedValue(float value) {
        if (value < 60) {
            return String.format("%.0fs", value); // Seconds
        } else if (value < 3600) {
            return String.format("%.0fm", value / 60); // Minutes
        } else {
            return String.format("%.0fh", value / 3600); // Hours
        }
    }
}
