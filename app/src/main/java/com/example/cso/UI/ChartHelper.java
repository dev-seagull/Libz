package com.example.cso.UI;

import static com.anychart.enums.Layout.HORIZONTAL;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.Text;
import com.anychart.core.cartesian.series.Bar;
import com.anychart.enums.TooltipDisplayMode;
import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

//
public class ChartHelper {

    public static LinearLayout createChartView(Context context){
        // Create a new LinearLayout to contain the chart
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500
        );
        params.gravity = Gravity.CENTER;
        layout.setLayoutParams(params);
        TextView title = new TextView(context);
        title.setText("15 MB to Sync");

        HorizontalBarChart barChart = new HorizontalBarChart(context);
        barChart.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.addView(title);
        layout.addView(barChart);

        setupHorizontalStackedBarChart(barChart);
        return layout;
    }

    private static void setupHorizontalStackedBarChart(HorizontalBarChart barChart) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, new float[]{400f, 300f, 500f})); // Sample stack

        // Create the data set and customize it
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(MainActivity.currentTheme.deviceStorageChartColors); // Dynamic colors from theme
        dataSet.setStackLabels(new String[]{"Media", "Free", "Others"}); // Labels for the stacks

        // Rounded corners workaround (Not supported natively by MPAndroidChart)
        // Custom BarRenderer logic can be added here if needed (not included in this code)

        // Create BarData object and set it to the chart
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(6f); // Bar width optimized for better visual

        // Set up the chart view with data
        barChart.setData(barData);

        // Customize the chart appearance
        barChart.getDescription().setEnabled(false); // Disable description text
        barChart.setFitBars(true); // Make the bars fit into the chart

        // Hide grid lines and axis to make the chart cleaner
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setEnabled(false); // Hide X-axis labels
        barChart.getAxisLeft().setDrawGridLines(false); // Remove grid lines
        barChart.getAxisLeft().setEnabled(false); // Disable the left Y-axis
        barChart.getAxisRight().setEnabled(false); // Disable the right Y-axis

        // Customize the legend
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false); // Ensure the legend is not drawn inside the chart

        // Refresh the chart
        barChart.invalidate();
    }
}
