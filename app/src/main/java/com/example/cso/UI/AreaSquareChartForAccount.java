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

import java.util.List;

public class AreaSquareChartForAccount {
    public static View createStorageChart(Context context, String userEmail) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
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

            if(used < synced){
                used = synced ;
            }

            RelativeLayout stackedSquaresLayout = new RelativeLayout(context);
            stackedSquaresLayout.setGravity(Gravity.CENTER);
            RelativeLayout.LayoutParams stackedParams = new RelativeLayout.LayoutParams(
                    300,
                    300
            );
            stackedSquaresLayout.setLayoutParams(stackedParams);

            LinearLayout subLayout = new LinearLayout(context);
            subLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams subLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            subLayout.setLayoutParams(subLayoutParams);

            createLabels(context,total, used, synced, subLayout);
            layout.addView(subLayout);

            createStackedSquares(context,synced, used, total,stackedSquaresLayout);
            AreaSquareChart.drawGridLines(stackedSquaresLayout,context);
            layout.addView(stackedSquaresLayout);
            stackedSquaresLayout.setPadding(0,0,37 + (UI.getDeviceWidth(context) / 12),0);
        }catch (Exception e){
            LogHandler.crashLog(e,"AccountAreaChart");
            return Details.getErrorAsChartAlternative(context);
        }

        return layout;
    }

    private static void addSquareToLayout(RelativeLayout layout, ImageView square, int size) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size * 3, size * 3);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_START);
        layout.addView(square, params);
    }


    private static void createLabels(Context context, double total, double used
            , double synced, LinearLayout parentLayout){
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

        View colorBox = new View(context);
        LinearLayout.LayoutParams colorBoxParams = new LinearLayout.LayoutParams((int) (UI.getDeviceHeight(context) * 0.008), (int) (UI.getDeviceHeight(context) * 0.008));
        colorBoxParams.setMargins(20, 15, 10, 0);
        colorBox.setLayoutParams(colorBoxParams);

        TextView totalText = new TextView(context);
        totalText.setText("Free: " + formattedTotal);
        totalText.setTextSize(12f);
        totalText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        totalText.setLayoutParams(totalParams);

        TextView usedText = new TextView(context);
        usedText.setText("Others: " + formattedUsed);
        usedText.setTextSize(12f);
        usedText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        usedText.getHeight();
        usedText.setLayoutParams(usedParams);

        TextView syncedText = new TextView(context);
        syncedText.setText("Buzzing Along: " + formattedSynced);
        syncedText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        syncedText.setTextSize(12f);
        syncedText.setLayoutParams(syncedParams);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        layout.setGravity(Gravity.BOTTOM);

        LinearLayout legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        colorBox.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
        colorBoxParams.setMargins(20, 15, 10, 0);
        legendItem.addView(colorBox);
        legendItem.addView(totalText);
        layout.addView(legendItem);

        colorBox = new View(context);
        colorBox.setLayoutParams(colorBoxParams);
        colorBox.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        colorBoxParams.setMargins(20, 15, 10, 0);
        legendItem.addView(colorBox);
        legendItem.addView(usedText);
        layout.addView(legendItem);

        colorBox = new View(context);
        colorBox.setLayoutParams(colorBoxParams);
        colorBox.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[3]);
        legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        colorBoxParams.setMargins(20, 15, 10, 0);
        legendItem.addView(colorBox);
        legendItem.addView(syncedText);
        layout.addView(legendItem);

        parentLayout.addView(layout);
//        parentLayout.setPadding(0,0,37 + (UI.getDeviceWidth(context) / 12),0);
        layout.setGravity(Gravity.BOTTOM);
    }

    public static String formatStorageSize(double sizeInMB) {
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

        square1.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
        square2.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        square3.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[3]);

        total = total / 1024;
        used = used / 1024;
        synced = synced / 1024;
        try{
            boolean isSquare2Zero = (used == 0.0);
            boolean isSquare3Zero = (synced == 0.0);
            boolean isSquare1EqualToSquare2 = (used == total);
            boolean isSquare2EqualToSquare3 = (used == synced);

            int[] numbers = new int[]{1,4,9,16,25,36,49,64,81,100};

            used = Math.round((used / total) * 100);
            synced = Math.round((synced / total) * 100);

            int minDiff = 102;
            double tempUsed = used;
            for(int number: numbers){
                int diff = (int) Math.abs(tempUsed - number);
                if (diff < minDiff) {
                    minDiff = diff;
                    used = number;
                }
            }

            minDiff = 102;
            double tempSynced = synced;
            for(int number: numbers){
                int diff = (int) Math.abs(tempSynced - number);
                if (diff < minDiff) {
                    minDiff = diff;
                    synced = number;
                }
            }

            if(isSquare2Zero){
                used = 0;
            }
            if(isSquare3Zero){
                synced = 0;
            }

            Log.d("AreaSquareAccountChart","333: " +  String.valueOf(used));
            Log.d("AreaSquareAccountChart", "333: " + String.valueOf(synced));

            used = Math.sqrt(used) * 10;
            synced = Math.sqrt(synced) * 10;

            Log.d("AreaSquareAccountChart","3333: " +  String.valueOf(used));
            Log.d("AreaSquareAccountChart", "3333: " + String.valueOf(synced));

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

            //total
            addSquareToLayout(layout,square1, 100);
            addSquareToLayout(layout,square2, (int) used);
            addSquareToLayout(layout,square3,(int) synced );

            Log.d("AreaSquareChart", String.valueOf(used));
            Log.d("AreaSquareChart", String.valueOf(synced));
        }catch (Exception e) { LogHandler.crashLog(e,"AreaSquareChart"); }
    }

}
