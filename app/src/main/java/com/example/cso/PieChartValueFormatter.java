package com.example.cso;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class PieChartValueFormatter  extends ValueFormatter {

    @Override
    public String getFormattedValue(float value) {
        return String.format("%.1f MB", value);
    }


}
