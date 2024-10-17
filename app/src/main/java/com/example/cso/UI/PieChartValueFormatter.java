package com.example.cso.UI;

import android.util.Log;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class PieChartValueFormatter  extends ValueFormatter {

    @Override
    public String getFormattedValue(float value) {
        if (value >= 100){
            return String.format("%.1f GB", value / 1024);
        }else if (value < 1){
            return String.format("%.1f KB", value * 1024);
        }
        return String.format("%.1f MB", value);
    }
    
}
