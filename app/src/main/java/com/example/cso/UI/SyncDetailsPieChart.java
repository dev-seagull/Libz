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

import java.util.ArrayList;
import java.util.List;

public class SyncDetailsPieChart {
    public static void addPieChartToSyncDetailsStatisticsLayout(Activity activity,
                                                                LinearLayout syncDetailsStatisticsLayout){
            activity.runOnUiThread(() -> {
                HorizontalBarChart stackedBarChart = new HorizontalBarChart(activity);
                View pieChartView = SyncDetailsPieChart.createStackedBarChart(activity, stackedBarChart);
                syncDetailsStatisticsLayout.addView(pieChartView);
                if(pieChartView instanceof  LinearLayout){
                    int width = (int) (UI.getDeviceWidth(activity) * 0.9);
                    int height = (int) (UI.getDeviceHeight(activity) * 0.085);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            width,
                            height
                    );
                    layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                    stackedBarChart.setLayoutParams(layoutParams);
                    LinearLayout chartLayout =  (LinearLayout) pieChartView;
                    chartLayout.setGravity(Gravity.CENTER);
                }
            });
    }

    public static LinearLayout createStackedBarChart(Context context, HorizontalBarChart stackedBarChart) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        double total =  DBHelper.getNumberOfAssets();
        double synced = DBHelper.getNumberOfSyncedAssets();
        double unsynced = total - synced;

        try {
            float[] stackedValues = new float[]{(float) synced, (float) unsynced};

            List<BarEntry> entries = new ArrayList<>();
            entries.add(new BarEntry(0f, stackedValues));

            int[] colors = MainActivity.currentTheme.syncDetailsPieChartColors;

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

            LinearLayout mainLegendLayout = new LinearLayout(context);
            mainLegendLayout.setOrientation(LinearLayout.VERTICAL);
            mainLegendLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            );
            LinearLayout legendLayout = new LinearLayout(context);
            legendLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            legendLayout.setGravity(Gravity.CENTER);
            legendParams.setMargins((int) (UI.getDeviceWidth(context) * 0.25),0,(int) (UI.getDeviceWidth(context) * 0.25),0);
            legendLayout.setLayoutParams(legendParams);

            int parentWidth = (int) UI.getDeviceWidth(context) ;

            Log.d("details", "synced: " + synced);
            Log.d("details", "UnSynced: " + unsynced);

            View legendItem = createLegendItem(context, "Buzzing along", colors[0], unsynced);
            legendItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            legendLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int totalWidth = legendLayout.getMeasuredWidth() + legendItem.getMeasuredWidth() + (int) (UI.getDeviceWidth(context) * 0.15) + (int) (UI.getDeviceWidth(context) * 0.15);

            Log.d("details", "total width: " + totalWidth);
            Log.d("details", "parent width: " + parentWidth);
            legendLayout.addView(legendItem);

            legendItem = createLegendItem(context, "Lagging behind", colors[1], synced);
            legendItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            legendLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalWidth = legendLayout.getMeasuredWidth() + legendItem.getMeasuredWidth() + (int) (UI.getDeviceWidth(context) * 0.15) + (int) (UI.getDeviceWidth(context) * 0.15);

            Log.d("details", "total width: " + totalWidth);
            Log.d("details", "parent width: " + parentWidth);
            if (totalWidth > parentWidth) {
                mainLegendLayout.addView(legendLayout);
                legendLayout = new LinearLayout(context);
                legendLayout.setOrientation(LinearLayout.HORIZONTAL);
                legendParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                legendLayout.setGravity(Gravity.CENTER);
                legendParams.setMargins((int) (UI.getDeviceWidth(context) * 0.25),0,(int) (UI.getDeviceWidth(context) * 0.25),0);
                legendLayout.setLayoutParams(legendParams);
            }

            legendLayout.addView(legendItem);

            mainLegendLayout.addView(legendLayout);
            layout.addView(mainLegendLayout);
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
        legendItem.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        legendItem.setGravity(Gravity.CENTER);

        View colorBox = new View(context);
        LinearLayout.LayoutParams colorBoxParams = new LinearLayout.LayoutParams((int) (UI.getDeviceHeight(context) * 0.008), (int) (UI.getDeviceHeight(context) * 0.008));
        colorBoxParams.setMargins(20, 2, 10, 0);
        colorBox.setLayoutParams(colorBoxParams);
        colorBox.setBackgroundColor(color);

        TextView labelText = new TextView(context);
        labelText.setText(label);
        int textColor = MainActivity.currentTheme.primaryTextColor;
        labelText.setTextColor(textColor);
        labelText.setTextSize(12f);

        legendItem.addView(colorBox);
        legendItem.addView(labelText);
        return legendItem;
    }

}
