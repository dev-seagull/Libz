package com.example.cso.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class GridLinesView extends View {

    private int numCols;
    private int numRows;
    private Paint paint;

    public GridLinesView(Context context, int numCols, int numRows) {
        super(context);
        this.numCols = numCols;
        this.numRows = numRows;
        init();
    }

    public GridLinesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(0xFF000000);
        paint.setStrokeWidth(3);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float cellWidth = width / numCols;
        float cellHeight = height / numRows;

        for (int i = 0; i <= numCols; i++) {
            float x = i * cellWidth;
            canvas.drawLine(x, 0, x, height, paint);
        }

        for (int i = 0; i <= numRows; i++) {
            float y = i * cellHeight;
            canvas.drawLine(0, y, width, y, paint);
        }
    }

    public static void drawGridLines(FrameLayout layout, Context context, int numCols, int numRows) {
        GridLinesView gridLinesView = new GridLinesView(context, numCols, numRows);
        layout.addView(gridLinesView);
    }
}
