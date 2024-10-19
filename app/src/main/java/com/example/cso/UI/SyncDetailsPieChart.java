package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cso.DBHelper;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SyncDetailsPieChart {

    public static View createPieChartView(PieChart pieChart, Activity activity){
        double totalAssets = DBHelper.getNumberOfAssets();
        double syncedAssets = DBHelper.getNumberOfSyncedAssets();
//        SyncDetailsPieChart.configurePieChartDimensions(pieChart, activity);
//        SyncDetailsPieChart.configurePieChartDataForSyncDetails(pieChart,syncedAssets,totalAssets - syncedAssets);
//        SyncDetailsPieChart.configurePieChartLegend(pieChart);
//        pieChart.invalidate();

        return pieChart;
    }

    public static void configurePieChartDataForSyncDetails(PieChart pieChart,double synced,double unsyncedMediaSize) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (synced != 0){
            entries.add(new PieEntry((float) synced, "Synced"));
        }
        if (unsyncedMediaSize != 0){
            entries.add(new PieEntry((int) unsyncedMediaSize, "UnSynced"));
        }
        configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);
        dataSet.setValueFormatter(new PieChartValueFormatter());

        int[] colors = MainActivity.currentTheme.syncDetailsPieChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setDrawValues(false);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }

    public static void configurePieChartDimensions(PieChart pieChart, Context context) {
        int width = (int) (UI.getDeviceWidth(context) * 0.35);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, width);
        params.setMargins(0,10,0,10);
        pieChart.setLayoutParams(params);
    }

    public static void configurePieChartLegend(PieChart pieChart) {
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
    }

    public static void createTextAreaForSyncDetailsPieChart(View chartLayout, LinearLayout layout, Context context){
        if(chartLayout instanceof PieChart){
            PieChart pieChart = (PieChart) chartLayout;
            PieData chartData = pieChart.getData();
            PieDataSet dataSet = (PieDataSet) chartData.getDataSet();
            List<PieEntry> entries = dataSet.getValues();
            int[] colors = MainActivity.currentTheme.syncDetailsPieChartColors;

            SpannableStringBuilder coloredText = new SpannableStringBuilder();

            for (int i = 0; i < entries.size(); i++) {
                PieEntry entry = entries.get(i);
                String label = entry.getLabel();
                int value = (int) entry.getValue();

                int colorIndex = i % colors.length;
                SpannableString entryText = new SpannableString(label + " assets count : " + value + "\n\n");
                entryText.setSpan(new ForegroundColorSpan(colors[colorIndex]), 0, entryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                coloredText.append(entryText);
            }

            TextView statusTextView = new TextView(context);
            statusTextView.setText(coloredText);
            statusTextView.setTextSize(10);
//            statusTextView.setPadding(32, 0, 0, 0);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            layoutParams.setMargins(0,10,0,10);
            statusTextView.setLayoutParams(layoutParams);

            layout.addView(statusTextView);
        }
    }

    public static void addPieChartToSyncDetailsStatisticsLayout(Activity activity,
                                                                LinearLayout syncDetailsStatisticsLayout){
        new Thread(() -> {
            ImageView[] loadingImage = new ImageView[]{new ImageView(activity)};
            MainActivity.activity.runOnUiThread(() -> {
                loadingImage[0].setBackgroundResource(R.drawable.yellow_loading);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(128,128);
                params.setMargins(0,64,0,64);
                loadingImage[0].setLayoutParams(params);
                syncDetailsStatisticsLayout.addView(loadingImage[0]);
            });
            activity.runOnUiThread(() -> {
                HorizontalBarChart stackedBarChart = new HorizontalBarChart(activity);
                View pieChartView = SyncDetailsPieChart.createStackedBarChart(activity, stackedBarChart);
                syncDetailsStatisticsLayout.addView(pieChartView);

//                if(pieChartView instanceof HorizontalBarChart){
//                    SyncDetailsPieChart.createTextAreaForSyncDetailsPieChart(pieChartView,syncDetailsStatisticsLayout,activity);
//                }

                syncDetailsStatisticsLayout.removeView(loadingImage[0]);
            });
        }).start();
    }

    public static LinearLayout createStackedBarChart(Context context, HorizontalBarChart stackedBarChart) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        double total =  DBHelper.getNumberOfAssets();
        double synced = DBHelper.getNumberOfSyncedAssets();
        double unsynced = total - synced;

        try {
            int width = (int) (UI.getDeviceWidth(context) * 0.8);
            int height = (int) (UI.getDeviceWidth(context) * 0.15);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
            stackedBarChart.setLayoutParams(layoutParams);


            float[] stackedValues = new float[]{(float) synced, (float) unsynced};

            List<BarEntry> entries = new ArrayList<>();
            entries.add(new BarEntry(0f, stackedValues));

            int[] colors = MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors;

            BarDataSet dataSet = new BarDataSet(entries, "Storage Usage");
            dataSet.setColors(colors);

            String[] stackLabels = new String[]{"synced", "unsynced"};
            dataSet.setStackLabels(stackLabels);

            BarData barData = new BarData(dataSet);
            stackedBarChart.setData(barData);

            stackedBarChart.getDescription().setEnabled(false);
            stackedBarChart.getLegend().setEnabled(false);
            XAxis xAxis = stackedBarChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawLabels(false);
            xAxis.setDrawAxisLine(false);
            YAxis leftAxis = stackedBarChart.getAxisLeft();
            leftAxis.setDrawGridLines(false);
            stackedBarChart.getAxisRight().setEnabled(false);
            leftAxis.setDrawLabels(false);
            leftAxis.setDrawAxisLine(false);
            dataSet.setDrawValues(false);

            layout.addView(stackedBarChart);

            LinearLayout legendLayout = new LinearLayout(context);
            legendLayout.setOrientation(LinearLayout.VERTICAL);
            legendLayout.setGravity(Gravity.CENTER);

            legendLayout.addView(createLegendItem(context, "UnSynced", colors[0], unsynced));
            legendLayout.addView(createLegendItem(context, "Synced", colors[1], synced));

            layout.addView(legendLayout);

        } catch (Exception e) {
            LogHandler.crashLog(e, "createStackedBarChart");
            layout.removeAllViews();
            layout.addView(Details.getErrorAsChartAlternative(context));
        }

        return layout;
    }
    private static LinearLayout createLegendItem(Context context, String label, int color, double value) {
        LinearLayout legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setGravity(Gravity.CENTER);

        View colorBox = new View(context);
        LinearLayout.LayoutParams colorBoxParams = new LinearLayout.LayoutParams(50, (int) (UI.getDeviceWidth(context) * 0.01));
        colorBoxParams.setMargins(10, 0, 10, 0);
        colorBox.setLayoutParams(colorBoxParams);
        colorBox.setBackgroundColor(color);

        TextView labelText = new TextView(context);
        labelText.setText(label+" : ");
        labelText.setTextSize((int) (UI.getDeviceWidth(context) * 0.01));
        labelText.setTextColor(MainActivity.currentTheme.primaryTextColor);

        TextView valueText = new TextView(context);
        valueText.setText(new PieChartValueFormatter().getFormattedValue((float) value));
        valueText.setTextSize((int) (UI.getDeviceWidth(context) * 0.01));
        valueText.setTextColor(MainActivity.currentTheme.primaryTextColor);

        legendItem.addView(colorBox);
        legendItem.addView(labelText);
        legendItem.addView(valueText);

        return legendItem;
    }

}
