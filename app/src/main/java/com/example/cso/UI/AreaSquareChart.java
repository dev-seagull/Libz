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

public class AreaSquareChart {
    public static View createStorageChart(Context context, JsonObject data) {
        int width =(int) (UI.getDeviceWidth(context) * 0.35);
        int total = (int) data.get("totalStorage").getAsDouble();
        int used = (int) ( data.get("usedSpace").getAsDouble() * width / total);
        int media = (int) ( data.get("mediaStorage").getAsDouble() * width / total);
        int synced = (int) ( data.get("syncedAssetsStorage").getAsDouble()  * width / total);
        Log.d("ui", "area chart total value: " + total);
        Log.d("ui", "area chart used value: " + used);
        Log.d("ui", "area chart media value: " + media);
        Log.d("ui", "area chart synced value: " + synced);

        total = Math.max(1, total);
        used = Math.max(1, used);
        media = Math.max(1, media);
        synced = Math.max(1, synced);
        total = (int) (Math.log10(total) / Math.log10(total) * width);
        used = (int) (Math.log10(used) / Math.log10(total) * width);
        media = (int) (Math.log10(media) / Math.log10(total) * width);
        synced = (int) (Math.log10(synced) / Math.log10(total) * width);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        layoutParams.setMargins(10, 10, 10, 10);
        layout.setLayoutParams(layoutParams);

        RelativeLayout temp = new RelativeLayout(context);
        temp.setGravity(Gravity.CENTER);

        RelativeLayout stackedSquaresLayout = createStackedSquares(context, total, used, media, synced);
        RelativeLayout.LayoutParams stackedParams = new RelativeLayout.LayoutParams(
                width,
                width
        );

        stackedParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        temp.addView(stackedSquaresLayout, stackedParams);

        LinearLayout linesLayout = new LinearLayout(context);
        linesLayout.setOrientation(LinearLayout.HORIZONTAL);
        RelativeLayout.LayoutParams linesParams = new RelativeLayout.LayoutParams(
                width,
                width
        );
        linesParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        linesParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        linesLayout.setLayoutParams(linesParams);

        int columnCount = Math.min(width / calculateGreatestCommonDivisor(total,used,media,synced),12);
        Log.d("ui", "area square chart column count: " + columnCount);
        drawGridLines(linesLayout, context, columnCount, columnCount);

        layout.addView(temp);
        stackedSquaresLayout.addView(linesLayout);
        LinearLayout labelsLayout = createLabels(context,data.get("totalStorage").getAsDouble()
                ,data.get("usedSpace").getAsDouble(),data.get("mediaStorage").getAsDouble(),
                data.get("syncedAssetsStorage").getAsDouble());
        layout.addView(labelsLayout);
        return layout;
    }

    public static RelativeLayout createStackedSquares(Context context, int square1Size, int square2Size, int square3Size, int square4Size) {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        ImageView square1 = new ImageView(context);
        ImageView square2 = new ImageView(context);
        ImageView square3 = new ImageView(context);
        ImageView square4 = new ImageView(context);

        square1.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
        square2.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        square3.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        square4.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[3]);

        addSquareToLayout(relativeLayout, square1, square1Size);
        addSquareToLayout(relativeLayout, square2, square2Size);
        addSquareToLayout(relativeLayout, square3, square3Size);
        addSquareToLayout(relativeLayout, square4, square4Size);

        return relativeLayout;
    }

    private static void addSquareToLayout(RelativeLayout layout, ImageView square, int size) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        layout.addView(square, params);
    }

    public static void drawGridLines(LinearLayout layout, Context context, int colNum, int rowNum) {

        View gridView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(1.5f);

                int width = getWidth();
                int height = getHeight();

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
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        gridParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        gridParams.addRule(RelativeLayout.ALIGN_PARENT_START);

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

        String formattedTotal = formatStorageSize(total);
        String formattedUsed = formatStorageSize(used);
        String formattedMedia = formatStorageSize(media);
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
        totalText.setText("Total: " + formattedTotal);
        totalText.setTextSize(10f);
        totalText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
        totalParams.setMargins(30, 0,0,0);
        totalText.setLayoutParams(totalParams);

        TextView usedText = new TextView(context);
        usedText.setText("Used: " + formattedUsed);
        usedText.setTextSize(10f);
        usedText.getHeight();
        usedText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        usedParams.setMargins(30,0,0,0);
        usedText.setLayoutParams(usedParams);

        TextView mediaText = new TextView(context);
        mediaText.setText("Media: " + formattedMedia);
        mediaText.setTextSize(10f);
        mediaText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        mediaParams.setMargins(30,0,0,0);
        mediaText.setLayoutParams(mediaParams);

        TextView syncedText = new TextView(context);
        syncedText.setText("Synced: " + formattedSynced);
        syncedText.setTextSize(10f);
        syncedText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[3]);
        syncedParams.setMargins(30,0 ,0,0);
        syncedText.setLayoutParams(syncedParams);

        layout.addView(totalText);
        layout.addView(usedText);
        layout.addView(mediaText);
        layout.addView(syncedText);
        return layout;
    }

    public static String formatStorageSize(double sizeInGB) {
        if (sizeInGB < 0.1) {
            double sizeInMB = sizeInGB * 1000;
            return String.format("%.1f MB", sizeInMB);
        } else {
            return String.format("%.1f GB", sizeInGB);
        }
    }

}
