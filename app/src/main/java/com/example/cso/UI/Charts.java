package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Charts {

    public static LinearLayout createDeviceDetailsLayout(Context context) {
        LinearLayout chartInnerLayout = new LinearLayout(context);
        chartInnerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT - 1000,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        chartInnerLayout.setOrientation(LinearLayout.VERTICAL);
        chartInnerLayout.setGravity(Gravity.CENTER);
        chartInnerLayout.setPadding(8, 8, 8, 8);
        chartInnerLayout.setElevation(4f);
        chartInnerLayout.setBackgroundResource(R.drawable.border_background);
        chartInnerLayout.setVisibility(View.GONE);
        return chartInnerLayout;
    }

    public static LinearLayout getDeviceButtonDetailsView(Button button){
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

    public static PieChart createPieChartForAccount(Activity context, String userEmail){
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart);
        configurePieChartDataForAccount(pieChart, userEmail);
        configurePieChartLegend(pieChart);
        configurePieChartInteractions(pieChart);
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

    public static PieChart createPieChartForDevice(Context context, DeviceHandler device) {
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart);
        configurePieChartDataForDevice(pieChart, device);
        configurePieChartLegend(pieChart);
        configurePieChartInteractions(pieChart);
        pieChart.invalidate();
        return pieChart;
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

    public static void configurePieChartDataForDevice(PieChart pieChart, DeviceHandler device) {
        JsonObject storageData = getDeviceStorageData(device);
        double freeSpace = storageData.get("freeSpace").getAsDouble() * 1000;
        double mediaStorage = storageData.get("mediaStorage").getAsDouble() * 1000;
        double usedSpaceExcludingMedia = storageData.get("usedSpaceExcludingMedia").getAsDouble() * 1000;

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) freeSpace, "Free Space"));
        entries.add(new PieEntry((float) mediaStorage, "Media"));
        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others"));

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

    public static JsonObject getDeviceStorageData(DeviceHandler device){
        JsonObject storageData = new JsonObject();
        StorageHandler storageHandler = new StorageHandler();
        double freeSpace = storageHandler.getFreeSpace();
        double totalStorage = storageHandler.getTotalStorage();
        double mediaStorage = Double.parseDouble(DBHelper.getPhotosAndVideosStorage());
        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;
        storageData.addProperty("freeSpace",freeSpace);
        storageData.addProperty("mediaStorage", mediaStorage);
        storageData.addProperty("usedSpaceExcludingMedia", usedSpaceExcludingMedia);
//
//        if (isCurrentDevice(device)){
//            StorageHandler storageHandler = new StorageHandler();
//            double freeSpace = storageHandler.getFreeSpace();
//            double totalStorage = storageHandler.getTotalStorage();
//            double mediaStorage = Double.parseDouble(DBHelper.getPhotosAndVideosStorage());
//            double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;
//            storageData.addProperty("freeSpace",freeSpace);
//            storageData.addProperty("mediaStorage", mediaStorage);
//            storageData.addProperty("usedSpaceExcludingMedia", usedSpaceExcludingMedia);
//
//        }else{
//            storageData = StorageSync.downloadStorageJsonFileFromAccounts(device);
//        }
        return storageData;
    }

    public static void configurePieChartLegend(PieChart pieChart) {
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
    }

    public static void configurePieChartInteractions(PieChart pieChart) {
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                handlePieChartSelection(pieChart,(int) h.getX());
            }

            @Override
            public void onNothingSelected() {

            }
        });
    }

    public static void handlePieChartSelection(PieChart pieChart, int index) {
        PieData pieData = pieChart.getData();
        PieDataSet pieDataSet = (PieDataSet) pieData.getDataSet();
        String label = pieDataSet.getEntryForIndex(index).getLabel();

        if ("Media(GB)".equals(label)) {
//            displayDirectoryUsage();
        } else {

        }
    }

    public static TextView createDirectoryUsageTextView(Context context){
        TextView directoryUsages = new TextView(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(3, 25, 3, 0);
        directoryUsages.setLayoutParams(textParams);
//        directoryUsages.setFontFamily(ResourcesCompat.getFont(context, R.font.sans_serif));
        directoryUsages.setGravity(Gravity.CENTER);
        directoryUsages.setTextSize(12);
        return directoryUsages;
    }

    public static LinearLayout getAccountButtonDetailsView(Button button){
        RelativeLayout accountButtonView = (RelativeLayout) button.getParent();
        LinearLayout parentLayout = (LinearLayout) accountButtonView.getParent();
        int deviceViewChildrenCount = parentLayout.getChildCount();
        for (int j = 0; j < deviceViewChildrenCount; j++) {
            View view = parentLayout.getChildAt(j);
            if (!(view instanceof RelativeLayout)) {
                return (LinearLayout) view;
            }
        }
        return null;
    }

}
