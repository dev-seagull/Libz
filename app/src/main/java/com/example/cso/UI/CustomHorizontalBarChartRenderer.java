package com.example.cso.UI;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class CustomHorizontalBarChartRenderer extends HorizontalBarChartRenderer {

    private float cornerRadius = 200;
    private Paint barPaint;

    public CustomHorizontalBarChartRenderer(BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
        barPaint = new Paint();
        barPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void drawExtras(Canvas c) {
        super.drawExtras(c);
        drawCustomBars(c);
    }

    private void drawCustomBars(Canvas c) {
        HorizontalBarChart chart = (HorizontalBarChart) mChart;
        BarData barData = chart.getBarData();
        if (barData == null) return;

        for (int i = 0; i < barData.getDataSetCount(); i++) {
            BarDataSet dataSet = (BarDataSet) barData.getDataSetByIndex(i);
            drawStackedBars(c, dataSet);
        }
    }

    private void drawStackedBars(Canvas c, BarDataSet dataSet) {
        for (int j = 0; j < dataSet.getEntryCount(); j++) {
            BarEntry entry = dataSet.getEntryForIndex(j);
            float[] vals = entry.getYVals();
            float cumulativeWidth = 0f;
            float yPosition = entry.getY();

            // Ensure the yPosition is calculated correctly based on the bar height
            float barHeight = 0.4f; // Set a height for the bars
            for (int k = 0; k < vals.length; k++) {
                float value = vals[k];
                if (value <= 0) continue;

                float left = cumulativeWidth;
                float right = cumulativeWidth + value;

                // Create a rounded rectangle for the bar
                RectF rect = new RectF(left, yPosition - barHeight / 2, right, yPosition + barHeight / 2);
                c.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint); // Draw rounded rectangle

                cumulativeWidth += value; // Move to the next segment
            }
        }
    }
}

