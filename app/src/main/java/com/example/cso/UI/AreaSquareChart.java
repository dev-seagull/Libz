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

import java.util.ConcurrentModificationException;

public class AreaSquareChart {
    public static LinearLayout createStorageChart(Context context, JsonObject data, String deviceId) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        layout.setLayoutParams(layoutParams);

        try{
            double total = data.get("totalStorage").getAsDouble();
            double used = data.get("usedSpace").getAsDouble() / total * 100;
            double media = data.get("mediaStorage").getAsDouble() / total * 100;
            double synced = data.get("syncedAssetsStorage").getAsDouble()  / total * 100;

            RelativeLayout stackedSquaresLayout = new RelativeLayout(context);
            RelativeLayout.LayoutParams stackedParams = new RelativeLayout.LayoutParams(
                    300,
                    300
            );
            stackedSquaresLayout.setLayoutParams(stackedParams);

            createStackedSquares(context,synced,media,used, stackedSquaresLayout);
            drawGridLines(stackedSquaresLayout, context);
            layoutParams.gravity = Gravity.LEFT;
            layoutParams.setMargins(5, 50, 0, 50);
            layout.addView(stackedSquaresLayout);

            String updateDate = DeviceStatusSync.getDeviceStatusLastUpdateTime(deviceId);
            LinearLayout updateDateLabelsLayout = createUpdateDateLabel(context, updateDate);
            LinearLayout.LayoutParams updateDateLabelsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            updateDateLabelsParams.setMargins(20,0,0,0);
            updateDateLabelsParams.gravity = Gravity.TOP;
            updateDateLabelsLayout.setLayoutParams(updateDateLabelsParams);

            LinearLayout labelsLayout = createLabels(context,data.get("totalStorage").getAsDouble()
                    ,data.get("usedSpace").getAsDouble(),data.get("mediaStorage").getAsDouble(),
                    data.get("syncedAssetsStorage").getAsDouble());
            LinearLayout.LayoutParams labelsLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            labelsLayoutParams.gravity = Gravity.BOTTOM ;
            labelsLayoutParams.setMargins(20,0,0,0);
            labelsLayout.setLayoutParams(labelsLayoutParams);

            LinearLayout temp = new LinearLayout(context);
            temp.setOrientation(LinearLayout.VERTICAL);
            temp.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            temp.addView(updateDateLabelsLayout);

            View spacer = new View(context);
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1
            );
            spacer.setLayoutParams(spacerParams);
            temp.addView(spacer);

            temp.addView(labelsLayout);

            layout.addView(temp);
        }catch (Exception e){
            LogHandler.crashLog(e,"AreaSquareChart");
            layout.removeAllViews();
            layout.addView(Details.getErrorAsChartAlternative(context));
        }
        return layout;
    }

    public static void createStackedSquares(Context context, double synced, double media, double used, RelativeLayout layout) {
        ImageView square1 = new ImageView(context);
        ImageView square2 = new ImageView(context);
        ImageView square3 = new ImageView(context);
        ImageView square4 = new ImageView(context);

        square1.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
        square2.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        square3.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        square4.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[3]);
        Log.d("AreaSquareChart","0: " +  String.valueOf(used));
        Log.d("AreaSquareChart", "0: " + String.valueOf(media));
        Log.d("AreaSquareChart", "0: " + String.valueOf(synced));
        try{
            boolean isSquare3Zero = (media == 0.0);
            boolean isSquare4Zero = (synced == 0.0);
            boolean isSquare2EqualToSquare3 = (used == media);
            boolean isSquare3EqualToSquare4 = (media == synced);

            used = (int) Math.round(used / 10);
            used = used * 10;
            media = (int) Math.round(media / 10);
            media = media * 10;
            synced = (int) Math.round(synced / 10);
            synced = synced * 10;

            if(!isSquare2EqualToSquare3 && media == used){
                media = media - 10;
                synced = synced - 10;
                Log.d("AreaSquareChart","change: " + media + " " + synced);
            }

            if(!isSquare3EqualToSquare4 && synced == media){
                synced = synced - 10;
                Log.d("AreaSquareChart","change2: " + media + " " + synced);
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

            Log.d("AreaSquareChart", String.valueOf(used));
            Log.d("AreaSquareChart", String.valueOf(media));
            Log.d("AreaSquareChart", String.valueOf(synced));
        }catch (Exception e) { LogHandler.crashLog(e,"AreaSquareChart"); }
    }

    private static void addSquareToLayout(RelativeLayout layout, ImageView square, int size) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size * 3, size * 3);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
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

    private static int calculateGreatestCommonDivisor(int a, int b, int c, int d) {
        try {
            int gcdAB = calculateGreatestCommonDivisor(a, b);
            int gcdCD = calculateGreatestCommonDivisor(c, d);
            int gcdResult = calculateGreatestCommonDivisor(gcdAB, gcdCD);
            Log.d("AreaChart",a + " " + b + " " + c + " " + d + " : " + gcdResult);
            return gcdResult;
        } catch (Exception e) {
            LogHandler.crashLog(e, "AreaChart");
        }
        return 1;
    }

    private static int calculateGreatestCommonDivisor(int a, int b){
        try{
            if(b == 0)
                return a;
            else
                return calculateGreatestCommonDivisor(b, a % b);
        }catch (Exception e){
            LogHandler.crashLog(e,"AreaChart");
        }
        return a;
    }

    private static LinearLayout createLabels(Context context, double total, double used, double media, double synced){
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        String formattedTotal = formatStorageSize(total - used);
        String formattedUsed = formatStorageSize(used - media);
        String formattedMedia = formatStorageSize(media - synced);
        String formattedSynced = formatStorageSize(synced);

        RelativeLayout.LayoutParams totalParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        RelativeLayout.LayoutParams usedParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        RelativeLayout.LayoutParams mediaParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        RelativeLayout.LayoutParams syncedParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        View colorBox = new View(context);
        LinearLayout.LayoutParams colorBoxParams = new LinearLayout.LayoutParams((int) (UI.getDeviceHeight(context) * 0.008), (int) (UI.getDeviceHeight(context) * 0.008));
        colorBoxParams.setMargins(5, 15, 10, 0);
        colorBox.setLayoutParams(colorBoxParams);

        TextView totalText = new TextView(context);
        totalText.setText("Free: " + formattedTotal);
        totalText.setTextSize(12f);
        totalText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        totalText.setLayoutParams(totalParams);

        TextView usedText = new TextView(context);
        usedText.setText("Others: " + formattedUsed);
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
        syncedText.setText("Buzzing Along: " + formattedSynced);
        syncedText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        syncedText.setTextSize(12f);
        syncedText.setLayoutParams(syncedParams);

        LinearLayout legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        layout.setGravity(Gravity.BOTTOM);
        colorBox.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
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
        legendItem.addView(colorBox);
        legendItem.addView(syncedText);
        layout.addView(legendItem);
        return layout;
    }

    public static LinearLayout createUpdateDateLabel(Context context, String updateDate){
        LinearLayout layout = new LinearLayout(context);

        TextView totalText = new TextView(context);
        totalText.setText(updateDate);
        totalText.setTextSize(11f);
        totalText.setTextColor(MainActivity.currentTheme.primaryTextColor);

        layout.addView(totalText);
        return layout;
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
