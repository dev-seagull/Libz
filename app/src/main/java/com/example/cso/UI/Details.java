package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
        layout.setBackgroundResource(R.drawable.border_background);
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
        layout.setBackgroundResource(R.drawable.border_background);
        return layout;
    }

    public static LinearLayout getDetailsView(Button button){
        RelativeLayout deviceButtonView = (RelativeLayout) button.getParent();
        LinearLayout parentLayout = (LinearLayout) deviceButtonView.getParent();
        int deviceViewChildrenCount = parentLayout.getChildCount();
        for (int j = 0; j < deviceViewChildrenCount; j++) {
            View view = parentLayout.getChildAt(j);
            if (!(view instanceof RelativeLayout)) {
                return (LinearLayout) view;
            }
        }
        return null;
    }

    public static PieChart createPieChartForDeviceStorageStatus(Context context, JsonObject data) {
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart);
        configurePieChartDataForDeviceStorageStatus(pieChart, data);
        configurePieChartLegend(pieChart);
//        configurePieChartInteractions(pieChart);
        pieChart.invalidate();
        return pieChart;
    }

    public static void configurePieChartDataForDeviceStorageStatus(PieChart pieChart, JsonObject storageData) {
        double freeSpace = storageData.get("freeSpace").getAsDouble() * 1000;
        double mediaStorage = storageData.get("mediaStorage").getAsDouble() * 1000;
        double usedSpaceExcludingMedia = storageData.get("usedSpaceExcludingMedia").getAsDouble() * 1000;

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) freeSpace, "Free Space"));
        entries.add(new PieEntry((float) mediaStorage, "Media"));
        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others"));

       configurePieChartDataFormatForDeviceStorageStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceStorageStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = {
                Color.parseColor("#00796B"),
                Color.parseColor("#004D40"),
                Color.parseColor("#80CBC4")
        };
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
        configurePieChartDimensions(pieChart);
        configurePieChartDataForDeviceSourceStatus(pieChart, data);
        configurePieChartLegend(pieChart);
        pieChart.invalidate();
        return pieChart;
    }

    public static void configurePieChartDataForDeviceSourceStatus(PieChart pieChart, JsonObject sourcesData) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (String source : sourcesData.keySet()){
            double size = sourcesData.get(source).getAsDouble() * 1000;
            entries.add(new PieEntry((float) size, source));
        }
        configurePieChartDataFormatForDeviceSourceStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSourceStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = {
                Color.parseColor("#00796B"),
                Color.parseColor("#004D40"),
                Color.parseColor("#80CBC4")
        };
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

    public static PieChart createPieChartForDeviceSyncedAssetsLocationStatus(Context context, JsonObject data){
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart);
        configurePieChartDataForDeviceSyncedAssetsLocationStatus(pieChart, data);
        configurePieChartLegend(pieChart);
        pieChart.invalidate();
        return pieChart;
    }

    public static void configurePieChartDataForDeviceSyncedAssetsLocationStatus(PieChart pieChart, JsonObject locationsData) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (String location : locationsData.keySet()){
            double locationSize = locationsData.get(location).getAsDouble();
            entries.add(new PieEntry((float) locationSize, location));
        }
        configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = {
                Color.parseColor("#00796B"),
                Color.parseColor("#004D40"),
                Color.parseColor("#80CBC4")
        };
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

    public static void configurePieChartDimensions(PieChart pieChart) {
        pieChart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        MainActivity.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int chartHeight = (int) (displayMetrics.heightPixels * 0.25);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                displayMetrics.widthPixels, chartHeight
        );

        pieChart.setLayoutParams(layoutParams);
    }

    public static void configurePieChartLegend(PieChart pieChart) {
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
    }

    public static PieChart createPieChartForAccount(Context context, String userEmail){
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart);
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
        int[] colors = {
                Color.parseColor("#1E88E5"),
                Color.parseColor("#304194")
//                ,Color.parseColor("#B3E5FC")
        };
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

<<<<<<< HEAD

    public static void createSyncDetailsLayout(Context context, Activity activity){
        LinearLayout syncButtonsLayout = activity.findViewById(R.id.syncButtonsLayout);
        LinearLayout syncDetailsLayout = createInnerDetailsLayout(context);
        ImageButton syncDetailsButton = activity.findViewById(R.id.syncDetailsButton);

        syncDetailsLayout.setVisibility(View.GONE);
        syncButtonsLayout.addView(syncDetailsLayout);

        TextView textView = new TextView(activity);
        textView.setText("Sync assets count: "+ DBHelper.countAndroidSyncedAssetsOnThisDevice(MainActivity.androidUniqueDeviceIdentifier));
        syncDetailsLayout.addView(textView);

        syncDetailsButton.setOnClickListener(view -> {
            if(syncDetailsLayout.getVisibility() == View.GONE){
                syncDetailsLayout.setVisibility(View.VISIBLE);
            }else{
                syncButtonsLayout.setVisibility(View.GONE);
            }
        });
    }

=======
    public static TextView createTextViewForEmptyDataSet(String text){
        TextView textView = new TextView(MainActivity.activity);
        textView.setText(text);
        textView.setTextSize(24f);
        textView.setGravity(Gravity.CENTER);
        return textView;
    }
>>>>>>> b506bc9519d21a1860c44dbee9821db44b3387bd
}









