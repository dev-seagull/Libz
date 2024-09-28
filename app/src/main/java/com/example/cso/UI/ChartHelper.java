package com.example.cso.UI;

import static com.anychart.enums.Layout.HORIZONTAL;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.BubbleDataEntry;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.charts.Scatter;
import com.anychart.core.Text;
import com.anychart.core.cartesian.series.Bar;
import com.anychart.data.Set;
import com.anychart.enums.TooltipDisplayMode;
import com.anychart.enums.TooltipPositionMode;
import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BubbleChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BubbleData;
import com.github.mikephil.charting.data.BubbleDataSet;
import com.github.mikephil.charting.data.BubbleEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//
public class ChartHelper {

    public static LinearLayout createChartView(Context context){
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
//        params.gravity = Gravity.CENTER;
        layout.setLayoutParams(params);
        TextView title = new TextView(context);
        title.setText("15 MB to Sync");

        HorizontalBarChart barChart = new HorizontalBarChart(context);
        barChart.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        layout.addView(barChart);
        setupHorizontalStackedBarChart(barChart);


        return layout;
    }

    private static void setupHorizontalStackedBarChart(HorizontalBarChart barChart) {
        CustomHorizontalBarChartRenderer customHorizontalBarChartRenderer = new CustomHorizontalBarChartRenderer(barChart, barChart.getAnimator(), barChart.getViewPortHandler());
        barChart.setRenderer(customHorizontalBarChartRenderer);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, new float[]{400f, 300f, 500f})); // Sample stack

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(MainActivity.currentTheme.deviceStorageChartColors); // Dynamic colors from theme
        dataSet.setStackLabels(new String[]{"Media", "Free", "Others"}); // Labels for the stacks

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(6f);


        barChart.setData(barData);


        barChart.getDescription().setEnabled(false); // Disable description text
        barChart.setFitBars(true);

        // Hide grid lines and axis to make the chart cleaner
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setEnabled(false);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);

        // Customize the legend
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false); // Ensure the legend is not drawn inside the chart

        barChart.invalidate();

    }
}




//    BubbleChart bubbleChart = new BubbleChart(context);
//        bubbleChart.setLayoutParams(new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
//
//                // Create entries for the bubble chart
//                ArrayList<BubbleEntry> entries = new ArrayList<>();
//        entries.add(new BubbleEntry(0f, 150f, 40f));
//        entries.add(new BubbleEntry(1f, 120f, 30f));
//        entries.add(new BubbleEntry(2f, 200f, 50f));
//        entries.add(new BubbleEntry(3f, 80f, 20f));
//        entries.add(new BubbleEntry(4f, 100f, 25f));
//        entries.add(new BubbleEntry(5f, 90f, 22f));
//
//        if (entries.size() > 5) {
//        Collections.sort(entries, new Comparator<BubbleEntry>() {
//@Override
//public int compare(BubbleEntry e1, BubbleEntry e2) {
//        return Float.compare(e2.getY(), e1.getY());
//        }
//        });
//
//        float othersValue = 0;
//        ArrayList<BubbleEntry> limitedEntries = new ArrayList<>();
//        for (int i = 0; i < entries.size(); i++) {
//        if (i < 4) {
//        limitedEntries.add(entries.get(i));
//        } else {
//        othersValue += entries.get(i).getY();
//        }
//        }
//
//        if (othersValue > 0) {
//        limitedEntries.add(new BubbleEntry(6f, othersValue, 50f));
//        }
//
//        entries = limitedEntries;
//        }
//
//        BubbleDataSet dataSet = new BubbleDataSet(entries, "Device Source Status");
//        dataSet.setColor(Color.BLUE); // Set bubble color
//        dataSet.setValueTextColor(Color.WHITE); // Set text color for bubble values
//        dataSet.setValueTextSize(12f); // Set text size for bubble values
//
//        BubbleData bubbleData = new BubbleData(dataSet);
//        bubbleChart.setData(bubbleData);
//        bubbleChart.getDescription().setEnabled(false);
//        bubbleChart.getAxisLeft().setEnabled(false);
//        bubbleChart.getAxisRight().setEnabled(false);
//        bubbleChart.getXAxis().setEnabled(false);
//        bubbleChart.setDrawGridBackground(false);
//        bubbleChart.setDrawBorders(false);
//        bubbleChart.setGridBackgroundColor(Color.TRANSPARENT);
//
//        bubbleChart.setDragEnabled(true);
//        bubbleChart.setScaleEnabled(true);
//        bubbleChart.setPinchZoom(true);
//        bubbleChart.getAxisLeft().setAxisMinimum(0f); // Minimum Y value
//        bubbleChart.getAxisLeft().setAxisMaximum(200f); // Maximum Y value
//        bubbleChart.getXAxis().setAxisMinimum(-1f); // Minimum X value
//        bubbleChart.getXAxis().setAxisMaximum(6f);
////        bubbleChart.setDrawValueAboveBubble(true); // Draw values above bubbles
//
//        // Add the bubble chart to the layout
//        layout.addView(bubbleChart);













//
//    BarChart barChart = new BarChart(context);
//        barChart.setLayoutParams(new LinearLayout.LayoutParams(
//                400,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//                ));
//                layout.addView(barChart);
//
//                ArrayList<BarEntry> entries = new ArrayList<>();
//        entries.add(new BarEntry(1, 20));
//        entries.add(new BarEntry(2, 15));
//        entries.add(new BarEntry(3, 10));
//
//
//        BarDataSet dataSet = new BarDataSet(entries, "");
//        dataSet.setColors(MainActivity.currentTheme.deviceStorageChartColors);
//
//
//        BarData barData = new BarData(dataSet);
//        barData.setBarWidth(1f);
//
//        barChart.setData(barData);
//        barChart.setFitBars(true);
//        barChart.getDescription().setEnabled(false);
//        barChart.setRotation(90f);
//
//        barChart.getAxisLeft().setDrawGridLines(false);
//        barChart.getAxisLeft().setEnabled(false);
//        barChart.getAxisRight().setEnabled(false);
//        barChart.getXAxis().setEnabled(false);
//        barChart.getXAxis().setDrawGridLines(false);
//
//        XAxis xAxis = barChart.getXAxis();
//        xAxis.setLabelRotationAngle(90f);
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setDrawLabels(true);
//        xAxis.setDrawGridLines(false);
//        xAxis.setGranularity(1f);
//        xAxis.setGranularityEnabled(true);
//        barChart.getLegend().setEnabled(false);

//
//
//    GridLayout treeMap = new GridLayout(context);
//        treeMap.setLayoutParams(new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//                ));
//                treeMap.setRowCount(2);
//                treeMap.setColumnCount(2);
//                int[] values = {40, 30, 20,10}; // Example values for the tree map
//                int[] colors = {Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW};
//                for (int i = 0; i < values.length; i++) {
//        Button section = new Button(context);
//        section.setBackgroundColor(colors[i]);
//        // Set layout params based on the values to represent size
//        GridLayout.LayoutParams gridparams = new GridLayout.LayoutParams();
//        gridparams.rowSpec = GridLayout.spec(i / 2, 1f); // Row number
//        gridparams.columnSpec = GridLayout.spec(i % 2, (float) values[i] / 100); // Column number and width based on value
//        section.setLayoutParams(gridparams);
//        section.setText(String.valueOf(values[i]));
//        section.setTextColor(Color.WHITE); // Text color
//
//        // Add the section to the tree map
//        treeMap.addView(section);
//        }
