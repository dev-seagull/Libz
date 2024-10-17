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

import com.example.cso.DBHelper;
import com.example.cso.GoogleDriveFolders;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.google.gson.JsonObject;

import java.util.List;

public class AreaSquareChartForAccount {
    public static int columnCount = 14;
    public static View createStorageChart(Context context, String userEmail) {
        int width =(int) (UI.getDeviceWidth(context) * 0.35);
        double[] dimensions;
        double total;
        double used;
        double synced;
        double earlyTotal;
        double earlyUsed;
        double earlySynced;
        try{
            dimensions = getRegularizedDimensions(width, userEmail);
            total = dimensions[0];
            used = dimensions[1];
            synced = dimensions[2];
            earlyTotal = dimensions[3];
            earlyUsed = dimensions[4];
            earlySynced = dimensions[5];
        }catch (Exception e){
            LogHandler.crashLog(e,"AccountAreaChart");
            return Details.getErrorAsChartAlternative(context);
        }

        LinearLayout[] layout = new LinearLayout[]{new LinearLayout(context)};
        MainActivity.activity.runOnUiThread(() -> {
            layout[0].setOrientation(LinearLayout.HORIZONTAL);
            layout[0].setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            layoutParams.setMargins(10, 10, 10, 10);
            layout[0].setLayoutParams(layoutParams);

            RelativeLayout temp = new RelativeLayout(context);
            temp.setGravity(Gravity.CENTER);

            RelativeLayout stackedSquaresLayout = createStackedSquares(context,(int) total,(int) used,(int) synced);
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
            drawGridLines(linesLayout, context, columnCount, columnCount);

            layout[0].addView(temp);
            stackedSquaresLayout.addView(linesLayout);
            LinearLayout labelsLayout = createLabels(context,earlyTotal, earlyUsed, earlySynced);
            layout[0].addView(labelsLayout);
        });

        return layout[0];
    }

    public static double[] getRegularizedDimensions(int width, String userEmail){

        double earlyTotal = 15;
        double earlyUsed = 0;
        double earlySynced = GoogleDriveFolders.getSizeOfAssetsFolder(userEmail);
        String[] columns = new String[] {"totalStorage","usedStorage","userEmail","type"};
        List<String[]> account_rows = DBHelper.getAccounts(columns);
        for (String[] account : account_rows){
            if (account[2].equals(userEmail) && account[3].equals("backup")){
                earlyTotal = Double.parseDouble(account[0]);
                earlyUsed = Double.parseDouble(account[1]);
                break;
            }
        }

        Log.d("AccountAreaChart","earlyTotal : " + earlyTotal);
        Log.d("AccountAreaChart", "earlyUsed : " + earlyUsed);
        Log.d("AccountAreaChart","earlySynced : " + earlySynced);
        double total = (earlyTotal * width / earlyTotal);
        double used = (earlyUsed * width / earlyTotal);
        double synced = (earlySynced * width / earlyTotal);

        total = Math.sqrt(total) * width / Math.sqrt(total);
        used = Math.sqrt(used)  * width / Math.sqrt(total);
        synced = Math.sqrt(synced) * width / Math.sqrt(total);

        total = Math.sqrt(total) * width / Math.sqrt(total);
        used = Math.sqrt(used)  * width / Math.sqrt(total);
        synced = Math.sqrt(synced) * width / Math.sqrt(total);

        Log.d("AccountAreaChart","total : " + total);
        Log.d("AccountAreaChart", "used : " + used);
        Log.d("AccountAreaChart","synced : " + synced);

        total = ((int)(total / columnCount)) * columnCount;
        Log.d("AreaSquareChart", "total after cast to int :  " + total);


        return new double[]{total,used,synced,earlyTotal,earlyUsed,earlySynced};
    }

    public static RelativeLayout createStackedSquares(Context context, int square1Size, int square2Size, int square3Size) {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        ImageView square1 = new ImageView(context);
        ImageView square2 = new ImageView(context);
        ImageView square3 = new ImageView(context);

        square1.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        square2.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        square3.setBackgroundColor(MainActivity.currentTheme.deviceStorageChartColors[3]);

        int width = square1Size;
        int distance = square1Size / columnCount;
        boolean isSquare2SizeSet = false;
        boolean isSquare3SizeSet = false;

        for (int i = 0; i <= square1Size + (2 * distance); i = i + distance ){
            if (i >= square3Size && !isSquare3SizeSet){
                square3Size = i;
                isSquare3SizeSet = true;
            }
            if (i >= square2Size && !isSquare2SizeSet){
                square2Size = i;
                isSquare2SizeSet = true;
            }
            if (i >= square1Size){
                square1Size = i;
                break;
            }
        }
        Log.d("AccountAreaChart", "after round area chart synced value: " + square3Size);
        Log.d("AccountAreaChart", "after round area chart used value: " + square2Size);
        Log.d("AccountAreaChart", "after round area chart total value: " + square1Size);



        square1Size = square1Size - (square1Size - width);
        square3Size = square3Size - (square1Size - width);
        square2Size = square2Size - (square1Size - width);
        square1Size = square1Size - (square1Size - width);

        Log.d("AreaSquareChart", "after round and scale area chart synced value: " + square3Size);
        Log.d("AreaSquareChart", "after round and scale area chart used value: " + square2Size);
        Log.d("AreaSquareChart", "after round and scale area chart total value: " + square1Size);


        addSquareToLayout(relativeLayout, square1, square1Size);
        addSquareToLayout(relativeLayout, square2, square2Size);
        addSquareToLayout(relativeLayout, square3, square3Size);

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

    private static int calculateGreatestCommonDivisor(int a, int b, int c) {
        try {
            int gcdAB = calculateGreatestCommonDivisor(a, b);
            int gcdResult = calculateGreatestCommonDivisor(gcdAB, c);
            Log.d("AreaChart",a + " " + b + " " + c + " : " + gcdResult);
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

    private static LinearLayout createLabels(Context context, double total, double used, double synced){
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        String formattedTotal = formatStorageSize(total);
        String formattedUsed = formatStorageSize(used);
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
        totalText.setText("Total: " + formattedTotal);
        totalText.setTextSize(10f);
        totalText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[1]);
        totalParams.setMargins(30, 0,0,0);
        totalText.setLayoutParams(totalParams);

        TextView usedText = new TextView(context);
        usedText.setText("Used: " + formattedUsed);
        usedText.setTextSize(10f);
        usedText.getHeight();
        usedText.setTextColor(MainActivity.currentTheme.deviceStorageChartColors[2]);
        usedParams.setMargins(30,0,0,0);
        usedText.setLayoutParams(usedParams);

        TextView syncedText = new TextView(context);
        syncedText.setText("Libz: " + formattedSynced);
        syncedText.setTextSize(10f);
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

}
