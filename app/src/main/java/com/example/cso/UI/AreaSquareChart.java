package com.example.cso.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.gson.JsonObject;

public class AreaSquareChart {

    public static View createChart(Context context, JsonObject data) {
        // Parse the data from the JsonObject
//        double total = 100; //data.get("total").getAsDouble();
//        double used = 80;   //data.get("used").getAsDouble();
//        double media = 50;  //data.get("media").getAsDouble();
//        double synced = 30; //data.get("synced").getAsDouble();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        layoutParams.setMargins(10, 10, 10, 10);
        layout.setLayoutParams(layoutParams);

        // Use RelativeLayout for temp so gridView can be added on top
        RelativeLayout temp = new RelativeLayout(context);
        temp.setGravity(Gravity.CENTER);

        // Create the stacked squares
        RelativeLayout stackedSquaresLayout = createStackedSquares(context, 300, 250, 200, 150);
        RelativeLayout.LayoutParams stackedParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        temp.addView(stackedSquaresLayout, stackedParams);

        // Draw grid lines on top of stacked squares
        drawGridLines(temp, context, 5, 5); // Adjust columns and rows here

        layout.addView(temp); // Add the RelativeLayout to the main LinearLayout
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

    public static void drawGridLines(RelativeLayout layout, Context context, int colNum, int rowNum) {
        // Create a custom View to handle the drawing of the grid
        View gridView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                // Prepare the Paint object for drawing the grid lines
                Paint paint = new Paint();
                paint.setColor(Color.BLACK); // Set grid line color (changeable)
                paint.setStrokeWidth(3);     // Set the thickness of the grid lines

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
}
