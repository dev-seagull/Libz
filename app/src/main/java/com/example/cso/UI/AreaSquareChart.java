package com.example.cso.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.view.ViewGroup;

import com.google.gson.JsonObject;

public class AreaSquareChart {

    public static View createChart(Context context, JsonObject data) {
        // Create the parent RelativeLayout
        RelativeLayout parentLayout = new RelativeLayout(context);
        RelativeLayout.LayoutParams parentLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        parentLayout.setLayoutParams(parentLayoutParams);

        // Create temp (RelativeLayout that will hold stacked squares)
        RelativeLayout temp = new RelativeLayout(context);
        RelativeLayout.LayoutParams tempParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        tempParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        parentLayout.addView(temp, tempParams);

        // Create the stacked squares
        RelativeLayout stackedSquaresLayout = createStackedSquares(context, 300, 250, 200, 150);
        temp.addView(stackedSquaresLayout); // Add the stacked squares to temp

        // Create temp2 (RelativeLayout that will hold the grid lines)
        RelativeLayout temp2 = new RelativeLayout(context);
        RelativeLayout.LayoutParams temp2Params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        temp2Params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        parentLayout.addView(temp2, temp2Params);

        // Draw grid lines in temp2
        drawGridLines(temp2, context, 5, 5); // Adjust grid size as needed

        return parentLayout; // Return the fully constructed layout tree
    }

    public static RelativeLayout createStackedSquares(Context context, int square1Size, int square2Size, int square3Size, int square4Size) {
        RelativeLayout relativeLayout = new RelativeLayout(context);

        // Create square1
        ImageView square1 = new ImageView(context);
        square1.setBackgroundColor(0xFFD3D3D3); // Light Gray
        RelativeLayout.LayoutParams square1Params = new RelativeLayout.LayoutParams(square1Size, square1Size);
        square1Params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        square1Params.addRule(RelativeLayout.ALIGN_PARENT_END);
        relativeLayout.addView(square1, square1Params);

        // Create square2 (inside square1)
        ImageView square2 = new ImageView(context);
        square2.setBackgroundColor(0xFFB0B0B0); // Medium Gray
        RelativeLayout.LayoutParams square2Params = new RelativeLayout.LayoutParams(square2Size, square2Size);
        square2Params.addRule(RelativeLayout.ALIGN_BOTTOM, square1.getId());
        square2Params.addRule(RelativeLayout.ALIGN_END, square1.getId());
        relativeLayout.addView(square2, square2Params);

        // Create square3 (inside square2)
        ImageView square3 = new ImageView(context);
        square3.setBackgroundColor(0xFF808080); // Dark Gray
        RelativeLayout.LayoutParams square3Params = new RelativeLayout.LayoutParams(square3Size, square3Size);
        square3Params.addRule(RelativeLayout.ALIGN_BOTTOM, square2.getId());
        square3Params.addRule(RelativeLayout.ALIGN_END, square2.getId());
        relativeLayout.addView(square3, square3Params);

        // Create square4 (inside square3)
        ImageView square4 = new ImageView(context);
        square4.setBackgroundColor(0xFF4F9EDB); // Blue
        RelativeLayout.LayoutParams square4Params = new RelativeLayout.LayoutParams(square4Size, square4Size);
        square4Params.addRule(RelativeLayout.ALIGN_BOTTOM, square3.getId());
        square4Params.addRule(RelativeLayout.ALIGN_END, square3.getId());
        relativeLayout.addView(square4, square4Params);

        return relativeLayout; // Return the stacked squares layout
    }

    public static void drawGridLines(RelativeLayout layout, Context context, int colNum, int rowNum) {
        // Create a custom View to handle the drawing of the grid lines
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

        // Add the gridView on top of temp2
        layout.addView(gridView, gridParams);
    }
}
