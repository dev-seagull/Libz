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

import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.google.gson.JsonObject;

import java.util.ConcurrentModificationException;

public class AreaSquareChart {
    public static LinearLayout createStorageChart(Context context, JsonObject data) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        layoutParams.setMargins(10, 10, 10, 30);
        layout.setLayoutParams(layoutParams);

        try{
            double total = data.get("totalStorage").getAsDouble();
            double used = data.get("usedSpace").getAsDouble() / total * 100;
            double media = data.get("mediaStorage").getAsDouble() / total * 100;
            double synced = data.get("syncedAssetsStorage").getAsDouble()  / total * 100;

            RelativeLayout stackedSquaresLayout = new RelativeLayout(context);
            stackedSquaresLayout.setGravity(Gravity.CENTER);
            RelativeLayout.LayoutParams stackedParams = new RelativeLayout.LayoutParams(
                    300,
                    300
            );
            stackedSquaresLayout.setLayoutParams(stackedParams);

            createStackedSquares(context,synced,media,used, stackedSquaresLayout);
            drawGridLines(stackedSquaresLayout, context);
            layout.addView(stackedSquaresLayout);

            LinearLayout labelsLayout = createLabels(context,data.get("totalStorage").getAsDouble()
                    ,data.get("usedSpace").getAsDouble(),data.get("mediaStorage").getAsDouble(),
                    data.get("syncedAssetsStorage").getAsDouble());
            layout.addView(labelsLayout);
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

        TextView totalText = new TextView(context);
        totalText.setText("Free: " + formattedTotal);
        totalText.setTextSize(10f);
        totalText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
        totalParams.setMargins(30, 0,0,0);
        totalText.setLayoutParams(totalParams);

        TextView usedText = new TextView(context);
        usedText.setText("Others: " + formattedUsed);
        usedText.setTextSize(10f);
        usedText.getHeight();
        usedText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        usedParams.setMargins(30,0,0,0);
        usedText.setLayoutParams(usedParams);

        TextView mediaText = new TextView(context);
        mediaText.setText("Lagging behind: " + formattedMedia);
        mediaText.setTextSize(10f);
        mediaText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        mediaParams.setMargins(30,0,0,0);
        mediaText.setLayoutParams(mediaParams);

        TextView syncedText = new TextView(context);
        syncedText.setText("Buzzing Along: " + formattedSynced);
        syncedText.setTextSize(10f);
        syncedText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[3]);
        syncedParams.setMargins(30,0 ,0,0);
        syncedText.setLayoutParams(syncedParams);

        layout.addView(totalText);
        layout.addView(usedText);
        layout.addView(mediaText);
        layout.addView(syncedText);
        layout.setPadding(0,60,0,0);
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
