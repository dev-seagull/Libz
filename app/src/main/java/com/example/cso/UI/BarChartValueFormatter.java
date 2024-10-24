package com.example.cso.UI;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class BarChartValueFormatter extends ValueFormatter {
    private final float total;

    public BarChartValueFormatter(float total) {
        this.total = total;
    }

    @Override
    public String getFormattedValue(float value) {
        float threshold = total * 0.25f;

        if (value < threshold) {
            return "";
        }

        if (value >= 100){
            return String.format("%.1f GB", value / 1024);
        }else if (value < 1){
            return String.format("%.1f KB", value * 1024);
        }
        return String.format("%.1f MB", value);
    }
}
