package com.example.cso.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cso.DeviceStatusSync;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.google.gson.JsonObject;

public class AreaSquareChart {
    public static LinearLayout createStorageChart(Context context, JsonObject data, String deviceId) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        try{
            double total = data.get("totalStorage").getAsDouble();
            double used = (data.get("usedSpace").getAsDouble() / total) * 100;
            double media = (data.get("mediaStorage").getAsDouble() / total) * 100;
            double synced = (data.get("syncedAssetsStorage").getAsDouble()  / total) * 100;

//            double total = 21.3;
//            double used = 20.4 / total * 100;
//            double media = 5.6 / total * 100;
//            double synced = 0;

            RelativeLayout stackedSquaresLayout = new RelativeLayout(context);
            RelativeLayout.LayoutParams stackedParams = new RelativeLayout.LayoutParams(
                    300,
                    300
            );
            stackedSquaresLayout.setLayoutParams(stackedParams);

            createStackedSquares(context,synced,media,used, total,stackedSquaresLayout);
            drawGridLines(stackedSquaresLayout, context);
            layout.addView(stackedSquaresLayout);

            LinearLayout subLayout = new LinearLayout(context);
            subLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams subLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            subLayout.setLayoutParams(subLayoutParams);
            String updateDate = DeviceStatusSync.getDeviceStatusLastUpdateTime(deviceId);
            createUpdateDateLabel(context, updateDate, subLayout, stackedSquaresLayout, layout);

            createLabels(context,data.get("totalStorage").getAsDouble()
                    ,data.get("usedSpace").getAsDouble(),data.get("mediaStorage").getAsDouble(),
                    data.get("syncedAssetsStorage").getAsDouble(), subLayout);

            layout.addView(subLayout);
        }catch (Exception e){
            LogHandler.crashLog(e,"AreaSquareChart");
            layout.removeAllViews();
            TextView textView = Details.getErrorAsChartAlternative(context);
            layout.addView(textView);
        }
        return layout;
    }

    public static void createStackedSquares(Context context, double synced, double media, double used,double total, RelativeLayout layout) {
        ImageView square1 = new ImageView(context);
        ImageView square2 = new ImageView(context);
        ImageView square3 = new ImageView(context);
        ImageView square4 = new ImageView(context);

        square1.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
        square2.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        square3.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        square4.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[3]);
        try{
            boolean isSquare3Zero = (media == 0.0);
            boolean isSquare4Zero = (synced == 0.0);
            boolean isSquare2EqualToSquare3 = (used == media);
            boolean isSquare3EqualToSquare4 = (media == synced);

            int[] numbers = new int[]{1,4,9,16,25,36,49,64,81};

            Log.d("AreaSquareChart","t0:" +  String.valueOf(used));
            Log.d("AreaSquareChart", "t0 :" + String.valueOf(media));
            Log.d("AreaSquareChart", "t0 :" + String.valueOf(synced));

            total = Math.round(total);
            used = Math.round(used);
            media = Math.round(media);
            synced = Math.round(synced);

            Log.d("AreaSquareChart","t1:" +  String.valueOf(used));
            Log.d("AreaSquareChart", "t1 :" + String.valueOf(media));
            Log.d("AreaSquareChart", "t1 :" + String.valueOf(synced));

            double minDiff = 10002;
            if(used != 100){
                double tempUsed = used;
                for(int number: numbers){
                    int diff = (int) Math.abs(tempUsed - number);
                    if (diff < minDiff) {
                        minDiff = diff;
                        Log.d("AreaSquareChart","Setting : " + number);
                        used = number;
                    }
                }
            }

            minDiff = 10002;
            if(media != 100){
                double tempMedia = media;
                for(int number: numbers){
                    int diff = (int) Math.abs(tempMedia - number);
                    if (diff < minDiff) {
                        minDiff = diff;
                        media = number;
                    }
                }
            }

            minDiff = 10002;
            if(synced != 100){
                double tempSynced = synced;
                for(int number: numbers){
                    int diff = (int) Math.abs(tempSynced - number);
                    if (diff < minDiff) {
                        minDiff = diff;
                        synced = number;
                    }
                }
            }

            if(isSquare3Zero){
                media = 0;
            }
            if(isSquare4Zero){
                synced = 0;
            }

            Log.d("AreaSquareChart","s0:" +  String.valueOf(used));
            Log.d("AreaSquareChart", "s0 :" + String.valueOf(media));
            Log.d("AreaSquareChart", "s0 :" + String.valueOf(synced));

            used = Math.sqrt(used) * 10;
            media = Math.sqrt(media) * 10;
            synced = Math.sqrt(synced) * 10;

            Log.d("AreaSquareChart","s:" +  String.valueOf(used));
            Log.d("AreaSquareChart", "s :" + String.valueOf(media));
            Log.d("AreaSquareChart", "s :" + String.valueOf(synced));

            if(!isSquare2EqualToSquare3 && media == used){
                media = media - 10;
                synced = synced - 10;
            }

            if(!isSquare3EqualToSquare4 && synced == media){
                synced = synced - 10;
            }

            if(!isSquare4Zero && synced < 10){
                synced = 10;
                if(isSquare3EqualToSquare4){
                    media = 10;
                    if(isSquare2EqualToSquare3){
                        used = 10;
                    }else{
                        if(used <= media){
                            used = 20;
                        }
                    }
                }else{
                    if(media <= 10){
                        media = 20;
                        if(isSquare2EqualToSquare3){
                            used = 20;
                        }else{
                            if(used <= media){
                                used = 30;
                            }
                        }
                    }
                }
            }else{
                if(!isSquare3Zero && media < 10 && isSquare4Zero){
                    media = 10;
                    if(isSquare3EqualToSquare4){
                        used = 10;
                    }else{
                        if(used <= 10){
                            used = 20;
                        }
                    }
                }
            }

            //total
            addSquareToLayout(layout,square1, 100);
            addSquareToLayout(layout,square2, (int) used);
            addSquareToLayout(layout,square3,(int) media);
            addSquareToLayout(layout,square4,(int) synced);

            Log.d("AreaSquareChart","f :" +  String.valueOf(used));
            Log.d("AreaSquareChart", "f :" + String.valueOf(media));
            Log.d("AreaSquareChart", "f :" + String.valueOf(synced));
        }catch (Exception e) { LogHandler.crashLog(e,"AreaSquareChart"); }
    }

    private static void addSquareToLayout(RelativeLayout layout, ImageView square, int size) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size * 3, size * 3);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_START);
        layout.addView(square, params);
    }

    public static void drawGridLines(RelativeLayout layout, Context context) {

        View gridView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(1.5f);

                int width = getWidth();
                int height = getHeight();
                int colNum = 10;
                int rowNum = 10;

                float cellWidth = (float) width / colNum;
                float cellHeight = (float) height / rowNum;

                for (int i = 0; i <= colNum; i++) {
                    float x = i * cellWidth;
                    canvas.drawLine(x, 0, x, height, paint);
                }

                for (int i = 0; i <= rowNum; i++) {
                    float y = i * cellHeight;
                    canvas.drawLine(0, y, width, y, paint);
                }
            }
        };

        RelativeLayout.LayoutParams gridParams = new RelativeLayout.LayoutParams(
                300,
                300
        );
        gridParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        gridParams.addRule(RelativeLayout.ALIGN_PARENT_END);

        layout.addView(gridView, gridParams);
    }

    private static void createLabels(Context context, double total, double used,
                                             double media, double synced, LinearLayout parentLayout){
        String formattedTotal = formatStorageSize(total - used);
        String formattedUsed = formatStorageSize(used - media);
        String formattedMedia = formatStorageSize(media - synced);
        String formattedSynced = formatStorageSize(synced);

        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        LinearLayout.LayoutParams usedParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        LinearLayout.LayoutParams syncedParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        View colorBox = new View(context);
        LinearLayout.LayoutParams colorBoxParams = new LinearLayout.LayoutParams(17, 17);
        colorBoxParams.setMargins(20, 15, 10, 0);
        colorBox.setLayoutParams(colorBoxParams);

        TextView totalText = new TextView(context);
        totalText.setText("Free: " + formattedTotal);
        totalText.setTextSize(12f);
        totalText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        totalText.setLayoutParams(totalParams);

        TextView usedText = new TextView(context);
        usedText.setText("Everything else: " + formattedUsed);
        usedText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        usedText.setTextSize(12f);
        usedText.getHeight();
        usedText.setLayoutParams(usedParams);

        TextView mediaText = new TextView(context);
        mediaText.setText("Lagging behind: " + formattedMedia);
        mediaText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        mediaText.setTextSize(12f);
        mediaText.setLayoutParams(mediaParams);

        TextView syncedText = new TextView(context);
        syncedText.setText("Buzzing along: " + formattedSynced);
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
        colorBox.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        colorBoxParams.setMargins(20, 15, 10, 0);
        legendItem.addView(colorBox);
        legendItem.addView(mediaText);
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
        parentLayout.setPadding((UI.getDeviceWidth(context) / 72),0,37 + (UI.getDeviceWidth(context) / 12),0);
        layout.setGravity(Gravity.BOTTOM);
    }

    public static void createUpdateDateLabel(Context context, String updateDate,
                                                     LinearLayout subLayout,
                                                     RelativeLayout stackedSquaresLayout, LinearLayout layout){
        TextView totalText = new TextView(context);
        totalText.setText(updateDate);
        totalText.setTextSize(11f);
        totalText.setTextColor(MainActivity.currentTheme.primaryTextColor);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.TOP;
        int topMargin = layout.getHeight() - stackedSquaresLayout.getHeight();
        Log.d("debug", String.valueOf(topMargin));
        layoutParams.setMargins(20,0,0,0);
        totalText.setLayoutParams(layoutParams);

        subLayout.addView(totalText);
        subLayout.setPadding(0,0,37 + (UI.getDeviceWidth(context) / 12),0);
        Log.d("debug", String.valueOf(subLayout.getHeight() - stackedSquaresLayout.getHeight()));
    }

    public static String formatStorageSize(double sizeInGB) {
        if (sizeInGB < 0.1) {
            double sizeInMB = sizeInGB * 1024;
            return String.format("%.1f MB", sizeInMB);
        } else {
            return String.format("%.1f GB", sizeInGB);
        }
    }


}
