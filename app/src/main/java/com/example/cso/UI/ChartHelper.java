package com.example.cso.UI;

import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

//
public class ChartHelper {

//    public static LinearLayout createChartView(Context context){
//        LinearLayout layout = new LinearLayout(context);
//        layout.setOrientation(LinearLayout.VERTICAL);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        );
////        params.gravity = Gravity.CENTER;
//        layout.setLayoutParams(params);
//        TextView title = new TextView(context);
//        title.setText("15 MB to Sync");
//
//        HorizontalBarChart barChart = new HorizontalBarChart(context);
//        barChart.setLayoutParams(new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        ));
//
//        layout.addView(barChart);
//        setupHorizontalStackedBarChart(barChart);
//
//
//        return layout;
//    }

    public static void setupHorizontalStackedBarChart(HorizontalBarChart barChart,double freeSpace
            ,double mediaStorage,double usedSpaceExcludingMedia) {

        CustomHorizontalBarChartRenderer customHorizontalBarChartRenderer = new CustomHorizontalBarChartRenderer(barChart, barChart.getAnimator(), barChart.getViewPortHandler());
        barChart.setRenderer(customHorizontalBarChartRenderer);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, new float[]{(float) mediaStorage, (float) freeSpace, (float) usedSpaceExcludingMedia})); // Sample stack

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(MainActivity.currentTheme.deviceStorageChartColors);
        dataSet.setStackLabels(new String[]{"Media", "Free", "Others"});
        dataSet.setValueFormatter(new PieChartValueFormatter());
        dataSet.setValueTextSize(12f);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(30f);


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
        legend.setTextSize(12f);

        barChart.setScaleY(1.3f);
        barChart.invalidate();

    }

    public static void setupHorizontalStackedAssetLocationBarChart(HorizontalBarChart barChart, JsonObject data) {

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();


        for (String location : data.keySet()) {
            double locationSize = data.get(location).getAsDouble();

            entries.add(new BarEntry(entries.size(), (float) locationSize));

            labels.add(location);
        }


        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(MainActivity.currentTheme.deviceStorageChartColors);
        dataSet.setStackLabels(labels.toArray(new String[0]));
        dataSet.setValueFormatter(new PieChartValueFormatter());
        dataSet.setValueTextSize(12f);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(30f);


        barChart.setData(barData);


        barChart.getDescription().setEnabled(false); // Disable description text
        barChart.setFitBars(true);

        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setEnabled(false);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);

        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        barChart.setScaleY(1.3f);
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
