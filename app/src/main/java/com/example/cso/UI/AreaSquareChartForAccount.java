package com.example.cso.UI;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cso.DBHelper;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AreaSquareChartForAccount {
    public static View createStorageChart(Context context, String userEmail) {
        LinearLayout layout = new LinearLayout(context);
        double total = 15;
        double used = 0;
        double synced;
        try{
            synced = DBHelper.getSizeOfSyncedAssetsFromAccount(userEmail);
            String[] columns = new String[] {"totalStorage","usedStorage","userEmail","type"};
            List<String[]> account_rows = DBHelper.getAccounts(columns);

            for (String[] account : account_rows){
                if (account[2].equals(userEmail) && account[3].equals("backup")){
                    total = Double.parseDouble(account[0]);
                    used = Double.parseDouble(account[1]);
                    break;
                }
            }

            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.LEFT);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            layoutParams.setMargins(100, 30, 0, 30);
            layout.setLayoutParams(layoutParams);

            RelativeLayout stackedSquaresLayout = new RelativeLayout(context);
            stackedSquaresLayout.setGravity(Gravity.CENTER);
            RelativeLayout.LayoutParams stackedParams = new RelativeLayout.LayoutParams(
                    300,
                    300
            );
            stackedSquaresLayout.setLayoutParams(stackedParams);
            createStackedSquares(context,synced, used, total,stackedSquaresLayout);
            AreaSquareChart.drawGridLines(stackedSquaresLayout,context);

            layout.addView(stackedSquaresLayout);
            LinearLayout labelsLayout = createLabels(context,total, used, synced);
            layout.setGravity(Gravity.BOTTOM);
            layout.setPadding(10,0,0,50);
            layout.addView(labelsLayout);
            
        }catch (Exception e){
            LogHandler.crashLog(e,"AccountAreaChart");
            return Details.getErrorAsChartAlternative(context);
        }

        return layout;
    }

    private static void addSquareToLayout(RelativeLayout layout, ImageView square, int size) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size * 3, size * 3);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        layout.addView(square, params);
    }


    private static LinearLayout createLabels(Context context, double total, double used, double synced){
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        String formattedTotal = formatStorageSize(total - used);
        String formattedUsed = formatStorageSize(used - synced);
        String formattedSynced = formatStorageSize(synced);

        RelativeLayout.LayoutParams totalParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        RelativeLayout.LayoutParams usedParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        RelativeLayout.LayoutParams syncedParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        TextView totalText = new TextView(context);
        totalText.setText("Free: " + formattedTotal);
        totalText.setTextSize(11f);
        totalText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        totalParams.setMargins(30, 0,0,0);
        totalText.setLayoutParams(totalParams);

        TextView usedText = new TextView(context);
        usedText.setText("Others: " + formattedUsed);
        usedText.setTextSize(11f);
        usedText.getHeight();
        usedText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        usedParams.setMargins(30,0,0,0);
        usedText.setLayoutParams(usedParams);

        TextView syncedText = new TextView(context);
        syncedText.setText("Libz: " + formattedSynced);
        syncedText.setTextSize(11f);
        syncedText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[3]);
        syncedParams.setMargins(30,0 ,0,0);
        syncedText.setLayoutParams(syncedParams);

        layout.addView(totalText);
        layout.addView(usedText);
        layout.addView(syncedText);
        return layout;
    }

    private static String formatStorageSize(double sizeInMB) {
        if (sizeInMB >= 100) {
            double sizeInGB = sizeInMB / 1024;
            return String.format("%.1f GB", sizeInGB);
        } else {
            return String.format("%.1f MB", sizeInMB);
        }
    }

    public static void createStackedSquares(Context context, double synced, double used,double total, RelativeLayout layout) {
        ImageView square1 = new ImageView(context);
        ImageView square2 = new ImageView(context);
        ImageView square3 = new ImageView(context);

        square1.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        square2.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        square3.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[3]);

        total = total / 1024;
        used = used / 1024;
        synced = synced / 1024;
        Log.d("AreaSquareAccountChart","0: " +  String.valueOf(total));
        Log.d("AreaSquareAccountChart","0: " +  String.valueOf(used));
        Log.d("AreaSquareAccountChart", "0: " + String.valueOf(synced));
        try{
            boolean isSquare2Zero = (used == 0.0);
            boolean isSquare3Zero = (synced == 0.0);
            boolean isSquare1EqualToSquare2 = (used == total);
            boolean isSquare2EqualToSquare3 = (used == synced);
//
            int x = (int) Math.round(100 / total);
            used = used * x;
            synced = synced * x;
            Log.d("AreaSquareAccountChart","1: " +  String.valueOf(x));
            Log.d("AreaSquareAccountChart","1: " +  String.valueOf(used));
            Log.d("AreaSquareAccountChart", "1: " + String.valueOf(synced));

            used = (int) Math.round(used / 10);
            used = used * 10;
            synced = (int) Math.round(synced / 10);
            synced = synced * 10;

            Log.d("AreaSquareAccountChart","2: " +  String.valueOf(used));
            Log.d("AreaSquareAccountChart", "2: " + String.valueOf(synced));

            if(!isSquare1EqualToSquare2 && used >= 100){
                used = 90;
                if(!isSquare2EqualToSquare3 && synced >= used){
                    synced = 80;
                }
            }

            if(!isSquare2EqualToSquare3 && synced >= used){
                synced = used - 10;
            }

            if(!isSquare3Zero && synced < 10){
                synced = 10;
                if(isSquare2EqualToSquare3){
                    used = 10;
                }else{
                    if(used <= 10){
                        used = 20;
                    }
                }
            }else{
                if(!isSquare2Zero && used < 10 && isSquare3Zero){
                    used = 10;
                }
            }
            Log.d("AreaSquareAccountChart","3: " +  String.valueOf(used));
            Log.d("AreaSquareAccountChart", "3: " + String.valueOf(synced));

            //total
            addSquareToLayout(layout,square1, 100);
            addSquareToLayout(layout,square2, (int) used);
            addSquareToLayout(layout,square3,(int) synced);

            Log.d("AreaSquareChart", String.valueOf(used));
            Log.d("AreaSquareChart", String.valueOf(synced));
        }catch (Exception e) { LogHandler.crashLog(e,"AreaSquareChart"); }
    }

}
