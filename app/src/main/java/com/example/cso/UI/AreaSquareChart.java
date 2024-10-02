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
import com.google.gson.JsonObject;

public class AreaSquareChart {

    public static View createChart(Context context, JsonObject data) {
        int width =(int) (UI.getDeviceWidth(context) * 0.35);
        int total = (int) 126.5; //data.get("total").getAsDouble();
        int used = (int) Math.log(51.2 / total);   //data.get("used").getAsDouble();
        int media = (int) Math.log(49.8 / total);  //data.get("media").getAsDouble();
        int synced = (int) Math.log(30.8 / total);  //data.get("synced").getAsDouble();
        
        width = total;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
//        layoutParams.gravity = Gravity.CENTER;
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
        drawGridLines(linesLayout, context, columnCount, columnCount);

        layout.addView(temp);
        stackedSquaresLayout.addView(linesLayout);
        LinearLayout labelsLayout = createLabels(context,total,used,media,synced);
        layout.addView(labelsLayout);
        return layout;
    }

    public static RelativeLayout createStackedSquares(Context context, int square1Size, int square2Size, int square3Size, int square4Size) {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        ImageView square1 = new ImageView(context);
        ImageView square2 = new ImageView(context);
        ImageView square3 = new ImageView(context);
        ImageView square4 = new ImageView(context);

        square1.setBackgroundColor(0xFFD3D3D3); // Light Gray
        square2.setBackgroundColor(0xFFB0B0B0); // Medium Gray
        square3.setBackgroundColor(0xFF808080); // Dark Gray
        square4.setBackgroundColor(0xFF4F9EDB); // Blue

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

                // Prepare the Paint object for drawing the grid lines
                Paint paint = new Paint();
                paint.setColor(Color.WHITE); // Set grid line color (changeable)
                paint.setStrokeWidth(1.5f);     // Set the thickness of the grid lines

                int width = getWidth();
                int height = getHeight();

                // Calculate the width of each cell based on the number of columns and rows
                float cellWidth = (float) width / colNum;
                float cellHeight = (float) height / rowNum;

                // Draw vertical lines
                for (int i = 0; i <= colNum; i++) {
                    float x = i * cellWidth;
                    canvas.drawLine(x, 0, x, height, paint); // Draw from (x, 0) to (x, height)
                }

                // Draw horizontal lines
                for (int i = 0; i <= rowNum; i++) {
                    float y = i * cellHeight;
                    canvas.drawLine(0, y, width, y, paint); // Draw from (0, y) to (width, y)
                }
            }
        };

        // Set layout parameters to match the parent size
        RelativeLayout.LayoutParams gridParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        gridParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        gridParams.addRule(RelativeLayout.ALIGN_PARENT_START);

        // Add the gridView on top of the stacked squares
        layout.addView(gridView, gridParams);
    }

    private static int calculateGreatestCommonDivisor(int a, int b, int c, int d) {
        try {
            // Calculate the GCD of pairs of numbers
            int gcdAB = calculateGreatestCommonDivisor(a, b);
            int gcdCD = calculateGreatestCommonDivisor(c, d);
            int gcdResult = calculateGreatestCommonDivisor(gcdAB, gcdCD);
            Log.d("AreaChart",a + " " + b + " " + c + " " + d + " : " + gcdResult);
            return gcdResult;
        } catch (Exception e) {
            LogHandler.crashLog(e, "AreaChart");
        }
        return 1;  // Return 1 in case of error (1 is the identity element for GCD)
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

    private static LinearLayout createLabels(Context context, int total, int used, int media, int synced){
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);


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
        totalText.setText("Total");
        totalText.setTextSize(10f);
        totalText.setTextColor(0xFFD3D3D3);
        totalParams.setMargins(10,(total - used) /2,0,0);
        totalText.setLayoutParams(totalParams);

        TextView usedText = new TextView(context);
        usedText.setText("Used");
        usedText.setTextSize(10f);
        usedText.getHeight();
        usedText.setTextColor(0xFFB0B0B0);
        usedParams.setMargins(10,0,0,0);
        usedText.setLayoutParams(usedParams);

        TextView mediaText = new TextView(context);
        mediaText.setText("Media");
        mediaText.setTextSize(10f);
        mediaText.setTextColor(0xFF808080);
        mediaParams.setMargins(10,0,0,(media - synced) /2 - 10);
        mediaText.setLayoutParams(mediaParams);

        TextView syncedText = new TextView(context);
        syncedText.setText("Synced");
        syncedText.setTextSize(10f);
        syncedText.setTextColor(0xFF4F9EDB);
        syncedParams.setMargins(10,synced/2 ,0,synced/2);
        syncedText.setLayoutParams(syncedParams);

        layout.addView(totalText);
        layout.addView(usedText);
        layout.addView(mediaText);
        layout.addView(syncedText);
        return layout;
    }
}
