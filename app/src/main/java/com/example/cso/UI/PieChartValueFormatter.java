package com.example.cso.UI;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class PieChartValueFormatter  extends ValueFormatter {

    @Override
    public String getFormattedValue(float value) {
        if (value >= 1000){
            return String.format("%.1f GB", value / 1000);
        }
        return String.format("%.1f MB", value);
    }


}
