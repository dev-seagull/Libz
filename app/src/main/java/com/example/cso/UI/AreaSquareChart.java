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
    public static int columnCount = 14;
    public static View createStorageChart(Context context, JsonObject data) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        layoutParams.setMargins(10, 10, 10, 10);
        layout.setLayoutParams(layoutParams);

        try{
            int width =(int) (UI.getDeviceWidth(context) * 0.35);
            double total = data.get("totalStorage").getAsDouble();
            double used = ( data.get("usedSpace").getAsDouble() * width / total);
            double media = ( data.get("mediaStorage").getAsDouble() * width / total);
            double synced =  ( data.get("syncedAssetsStorage").getAsDouble()  * width / total);

            Log.d("AreaSquareChart", "area chart total value: " + total);
            Log.d("AreaSquareChart", "area chart used value: " + used);
            Log.d("AreaSquareChart", "area chart media value: " + media);
            Log.d("AreaSquareChart", "area chart synced value: " + synced);

            total = Math.sqrt(total) * width / Math.sqrt(total);
            used = Math.sqrt(used)  * width / Math.sqrt(total);
            media = Math.sqrt(media) * width / Math.sqrt(total);
            synced = Math.sqrt(synced) * width / Math.sqrt(total);

            Log.d("AreaSquareChart", "after2 area chart total value: " + total);
            Log.d("AreaSquareChart", "after2 area chart used value: " + used);
            Log.d("AreaSquareChart", "after2 area chart media value: " + media);
            Log.d("AreaSquareChart", "after2 area chart synced value: " + synced);

            total = Math.sqrt(total) * width / Math.sqrt(total);
            used = Math.sqrt(used)  * width / Math.sqrt(total);
            media = Math.sqrt(media) * width / Math.sqrt(total);
            synced = Math.sqrt(synced) * width / Math.sqrt(total);

            Log.d("AreaSquareChart", "after area chart total value: " + total);
            Log.d("AreaSquareChart", "after area chart used value: " + used);
            Log.d("AreaSquareChart", "after area chart media value: " + media);
            Log.d("AreaSquareChart", "after area chart synced value: " + synced);
            Log.d("AreaSquareChart", "total before cast to int :  " + total);
            total = ((int)(total / columnCount)) * columnCount;
            Log.d("AreaSquareChart", "total after cast to int :  " + total);
            RelativeLayout temp = new RelativeLayout(context);
            temp.setGravity(Gravity.CENTER);

            RelativeLayout stackedSquaresLayout = createStackedSquares(context,(int) synced,(int) media,(int) used,(int) total);
            RelativeLayout.LayoutParams stackedParams = new RelativeLayout.LayoutParams(
                    (int) total,
                    (int) total
            );

            stackedParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            temp.addView(stackedSquaresLayout, stackedParams);

            LinearLayout linesLayout = new LinearLayout(context);
            linesLayout.setOrientation(LinearLayout.HORIZONTAL);
            RelativeLayout.LayoutParams linesParams = new RelativeLayout.LayoutParams(
                    (int) total,
                    (int) total
            );
            linesParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            linesParams.addRule(RelativeLayout.CENTER_IN_PARENT);

            linesLayout.setLayoutParams(linesParams);


            Log.d("AreaSquareChart", "area square chart column count: " + columnCount);
            drawGridLines(linesLayout, context, columnCount, columnCount);

            layout.addView(temp);
            stackedSquaresLayout.addView(linesLayout);
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

    public static RelativeLayout createStackedSquares(Context context, int square1Size, int square2Size, int square3Size, int square4Size) {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        ImageView square1 = new ImageView(context);
        ImageView square2 = new ImageView(context);
        ImageView square3 = new ImageView(context);
        ImageView square4 = new ImageView(context);

        square1.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[3]);
        square2.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        square3.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        square4.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[0]);
        int width = square4Size;
        int distance = square4Size / columnCount;
        boolean isSquare1SizeSet = false;
        boolean isSquare2SizeSet = false;
        boolean isSquare3SizeSet = false;

        for (int i = 0; i < square4Size + (3 * distance); i = i + distance ){
            if (i >= square1Size && !isSquare1SizeSet){
                square1Size = i;
                isSquare1SizeSet = true;
//                continue;
            }
            if (i >= square2Size && !isSquare2SizeSet){
                square2Size = i;
                isSquare2SizeSet = true;
//                continue;
            }
            if (i >= square3Size && !isSquare3SizeSet){
                square3Size = i;
                isSquare3SizeSet = true;
//                continue;
            }
            if (i >= square4Size){
                square4Size = i;
                break;
            }
        }
        Log.d("AreaSquareChart", "after round area chart total value: " + square4Size);
        Log.d("AreaSquareChart", "after round area chart used value: " + square3Size);
        Log.d("AreaSquareChart", "after round area chart media value: " + square2Size);
        Log.d("AreaSquareChart", "after round area chart synced value: " + square1Size);



        square4Size = square4Size - (square4Size - width);
        square3Size = square3Size - (square4Size - width);
        square2Size = square2Size - (square4Size - width);
        square1Size = square1Size - (square4Size - width);

        Log.d("AreaSquareChart", "after round and scale area chart total value: " + square4Size);
        Log.d("AreaSquareChart", "after round and scale area chart used value: " + square3Size);
        Log.d("AreaSquareChart", "after round and scale area chart media value: " + square2Size);
        Log.d("AreaSquareChart", "after round and scale area chart synced value: " + square1Size);



        addSquareToLayout(relativeLayout, square4, square4Size);
        addSquareToLayout(relativeLayout, square3, square3Size);
        addSquareToLayout(relativeLayout, square2, square2Size);
        addSquareToLayout(relativeLayout, square1, square1Size);

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
            double sizeInMB = sizeInGB * 1024;
            return String.format("%.1f MB", sizeInMB);
        } else {
            return String.format("%.1f GB", sizeInGB);
        }
    }


}
