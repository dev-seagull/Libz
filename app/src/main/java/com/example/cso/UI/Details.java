package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.BubbleDataEntry;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.charts.Scatter;
import com.anychart.core.cartesian.series.Bubble;
import com.anychart.data.Set;
import com.anychart.enums.TooltipPositionMode;
import com.example.cso.DBHelper;
import com.example.cso.DeviceHandler;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.StorageHandler;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Details {

    public static LinearLayout createDetailsLayout(Context context) {
        LinearLayout layout = new LinearLayout(context);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(40,16,40,0);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);

        layout.setElevation(4f);
        GradientDrawable gradientDrawable = UI.createBorderInnerLayoutDrawable(context);
        layout.setBackground(gradientDrawable);
        layout.setVisibility(View.GONE);
        return layout;
    }

    public static LinearLayout createInnerDetailsLayout(Context context) {
        LinearLayout layout = new LinearLayout(context);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        layout.setPadding(8, 8, 8, 8);
        layout.setElevation(4f);
        GradientDrawable gradientDrawable = UI.createBorderInnerLayoutDrawable(context);
        layout.setBackground(gradientDrawable);
        return layout;
    }

    public static FrameLayout getDetailsView(Button button){
        ViewParent deviceButtonView = button.getParent();
        if (deviceButtonView instanceof RelativeLayout){
            RelativeLayout deviceButtonRelativeLayout = (RelativeLayout) deviceButtonView;
            ViewParent parentLayout = deviceButtonRelativeLayout.getParent();
            if (parentLayout instanceof LinearLayout){
                LinearLayout parentLinearLayout = (LinearLayout) parentLayout;
                int deviceViewChildrenCount = parentLinearLayout.getChildCount();
                for (int j = 0; j < deviceViewChildrenCount; j++) {
                    View view = parentLinearLayout.getChildAt(j);
                    if (!(view instanceof RelativeLayout)) {
                        return (FrameLayout) view;
                    }
                }
            }
        }
        return null;
    }

    public static PieChart createPieChartForDeviceStorageStatus(Context context, JsonObject data) {
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart, context);
        if (data != null && data.size() > 0){
            configurePieChartDataForDeviceStorageStatus(pieChart, data);
            configurePieChartLegend(pieChart);
//            configurePieChartInteractions(pieChart);
        }
        pieChart.invalidate();
        return pieChart;
    }

    public static void configurePieChartDataForDeviceStorageStatus(PieChart pieChart, JsonObject storageData) {
        double freeSpace = storageData.get("freeSpace").getAsDouble() * 1024;
        double mediaStorage = storageData.get("mediaStorage").getAsDouble() * 1024;
        double usedSpaceExcludingMedia = storageData.get("usedSpaceExcludingMedia").getAsDouble() * 1024;

        ArrayList<PieEntry> entries = new ArrayList<>();

        entries.add(new PieEntry((float) freeSpace, "Free Space"));
        entries.add(new PieEntry((float) mediaStorage, "Media"));
        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others"));

       configurePieChartDataFormatForDeviceStorageStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceStorageStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);
        int[] colors = MainActivity.currentTheme.deviceStorageChartColors;

        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        // Enable value lines and set value positions
        dataSet.setDrawValues(true);
        dataSet.setValueLinePart1OffsetPercentage(80f); // Offset of the line
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueLineWidth(2f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);


        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }

//    public static void configurePieChartInteractions(PieChart pieChart) {
//        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
//            @Override
//            public void onValueSelected(Entry e, Highlight h) {
//                handlePieChartSelection(pieChart,(int) h.getX());
//            }
//
//            @Override
//            public void onNothingSelected() {
//
//            }
//        });
//    }

//    public static void handlePieChartSelection(PieChart pieChart, int index) {
//        PieData pieData = pieChart.getData();
//        PieDataSet pieDataSet = (PieDataSet) pieData.getDataSet();
//        String label = pieDataSet.getEntryForIndex(index).getLabel();
//
//        if ("Media(GB)".equals(label)) {
////            displayDirectoryUsage();
//        } else {
//
//        }
//    }

//    public static TextView createDirectoryUsageTextView(Context context){
//        TextView directoryUsages = new TextView(context);
//        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//        );
//        directoryUsages.setLayoutParams(textParams);
////        directoryUsages.setFontFamily(ResourcesCompat.getFont(context, R.font.sans_serif));
//        directoryUsages.setGravity(Gravity.CENTER);
//        directoryUsages.setTextSize(12);
//        return directoryUsages;
//    }

    public static PieChart createPieChartForDeviceSourceStatus(Context context, JsonObject data) {
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart, context);
        if (data != null && data.size() > 0){
            configurePieChartDataForDeviceSourceStatus(pieChart, data);
            configurePieChartLegend(pieChart);
        }
        pieChart.invalidate();
        return pieChart;
    }

    public static void configurePieChartDataForDeviceSourceStatus(PieChart pieChart, JsonObject sourcesData) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (String source : sourcesData.keySet()){
            double size = sourcesData.get(source).getAsDouble();
            entries.add(new PieEntry((float) size, source));
        }
        configurePieChartDataFormatForDeviceSourceStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSourceStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        if (entries.size() > 5) {
            Collections.sort(entries, new Comparator<PieEntry>() {
                @Override
                public int compare(PieEntry e1, PieEntry e2) {
                    return Float.compare(e2.getValue(), e1.getValue());
                }
            });

            float othersValue = 0;
            ArrayList<PieEntry> limitedEntries = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                if (i < 4) {
                    limitedEntries.add(entries.get(i));
                } else {
                    othersValue += entries.get(i).getValue();
                }
            }

            if (othersValue > 0) {
                limitedEntries.add(new PieEntry(othersValue, "Others"));
            }
            entries = limitedEntries;
        }

        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        dataSet.setDrawValues(true);
        dataSet.setValueLinePart1OffsetPercentage(200f);
        dataSet.setValueLinePart1Length(0.5f);
        dataSet.setValueLinePart2Length(0.8f);
        dataSet.setValueLineWidth(2f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(100f);
        dataSet.setValueLineVariableLength(true);


        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(false);
    }


    public static PieChart createPieChartForDeviceSyncedAssetsLocationStatus(Context context, JsonObject data){
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart, context);
        if (data != null && data.size() > 0){
            configurePieChartDataForDeviceSyncedAssetsLocationStatus(pieChart, data);
            configurePieChartLegend(pieChart);
        }
        pieChart.invalidate();
        return pieChart;
    }

    public static void configurePieChartDataForDeviceSyncedAssetsLocationStatus(PieChart pieChart, JsonObject locationsData) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (String location : locationsData.keySet()){
            double locationSize = locationsData.get(location).getAsDouble();
            Log.d("DeviceStatusSync","location size for " + location + " is " + locationSize );
            entries.add(new PieEntry((float) locationSize , location));
        }
        configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        // Enable value lines and set value positions
        dataSet.setDrawValues(true);
        dataSet.setValueLinePart1OffsetPercentage(80f); // Offset of the line
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueLineWidth(2f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);


        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }

    public static void configurePieChartDimensions(PieChart pieChart, Context context) {
        int width =(int) (UI.getDeviceWidth(context) * 0.35);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width);
        params.setMargins(0,10,0,10);
        pieChart.setLayoutParams(params);
    }

    public static void configurePieChartLegend(PieChart pieChart) {
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
    }

    public static PieChart createPieChartForAccount(Context context, String userEmail){
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart, context);
        configurePieChartDataForAccount(pieChart, userEmail);
        configurePieChartLegend(pieChart);
//        configurePieChartInteractions(pieChart);
        pieChart.invalidate();
        return pieChart;
    }

    public static void configurePieChartDataForAccount(PieChart pieChart, String userEmail) {
        String[] columns = new String[] {"totalStorage","usedStorage","userEmail","type"};
        List<String[]> account_rows = DBHelper.getAccounts(columns);
        double freeSpace =0;
        double usedStorage = 0;
        for (String[] account : account_rows){
            if (account[2].equals(userEmail) && account[3].equals("backup")){
                double totalStorage = Double.parseDouble(account[0]);
                usedStorage = Double.parseDouble(account[1]);
                freeSpace = totalStorage - usedStorage;
                break;
            }
        }
//        JsonObject storageData = getDeviceStorageData(device);
//        double freeSpace = storageData.get("freeSpace").getAsDouble();
//        double mediaStorage = storageData.get("mediaStorage").getAsDouble();
//        double usedSpaceExcludingMedia = storageData.get("usedSpaceExcludingMedia").getAsDouble();

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) freeSpace, "Free Space"));
        entries.add(new PieEntry((float) usedStorage, "Used Storage"));
//        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others(GB)"));

        PieDataSet dataSet = new PieDataSet(entries, null);
        int[] colors = MainActivity.currentTheme.accountStorageDataChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        // Enable value lines and set value positions
        dataSet.setDrawValues(true);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }

    public static ImageButton createRightArrowButton(Context context){
        ImageButton rightImageButton = new ImageButton(context);
        rightImageButton.setImageResource(R.drawable.right);
        rightImageButton.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams rightButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rightImageButton.setPadding(0,0,30,0);
        rightImageButton.setScaleX(1.75f);
        rightImageButton.setScaleY(1.75f);
        rightImageButton.setLayoutParams(rightButtonParams);
        FrameLayout.LayoutParams rightButtonLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rightButtonLayoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        rightImageButton.setLayoutParams(rightButtonLayoutParams);
        return rightImageButton;
    }

    public static ImageButton createLeftArrowButton(Context context){
        ImageButton leftImageButton = new ImageButton(context);
        leftImageButton.setImageResource(R.drawable.left);
        leftImageButton.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams leftButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        leftImageButton.setPadding(30,0,0,0);
        leftImageButton.setScaleX(1.75f);
        leftImageButton.setScaleY(1.75f);
        leftImageButton.setLayoutParams(leftButtonParams);
        FrameLayout.LayoutParams leftButtonLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        leftButtonLayoutParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        leftImageButton.setLayoutParams(leftButtonLayoutParams);
        return leftImageButton;
    }

    public static FrameLayout createFrameLayoutForButtonDetails(Context context, String type, String buttonId){
        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        frameParams.setMargins(40,16,40,0);
        frameLayout.setLayoutParams(frameParams);
        frameLayout.setElevation(4f);
        frameLayout.setVisibility(View.GONE);

        ViewPager2 viewPager = DetailsViewPager.createViewerPage(context, buttonId, type);
        frameLayout.addView(viewPager);

        ImageButton leftArrowButton = createLeftArrowButton(context);
        leftArrowButton.setOnClickListener(v -> {
            int previousPage = viewPager.getCurrentItem() - 1;
            viewPager.setCurrentItem(previousPage, true);
        });
        frameLayout.addView(leftArrowButton);

        ImageButton rightArrowButton = createRightArrowButton(context);
        rightArrowButton.setOnClickListener(v -> {
            int nextPage = viewPager.getCurrentItem() + 1;
            viewPager.setCurrentItem(nextPage, true);
        });
        frameLayout.addView(rightArrowButton);

        return frameLayout;

    }

//    public static LinearLayout createLoadingLayout(Context context){
//        LinearLayout layout = createInnerDetailsLayout(context);
//
//    }

}









