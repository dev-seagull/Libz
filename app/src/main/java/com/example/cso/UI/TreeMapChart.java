package com.example.cso.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class TreeMapChart extends View {

    private Paint paint;
    private float totalSpace = 1000; // Total space
    private float usedSpace = 600; // Used space
    private float mediaSpace = 300; // Media within used space
    private float syncedMediaSpace = 150; // Synced media within media space

    public TreeMapChart(Context context) {
        super(context);
        init();
    }

    public TreeMapChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TreeMapChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Set a default size if none is provided
        int desiredWidth = 500;
        int desiredHeight = 500;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        // Measure width
        if (widthMode == MeasureSpec.EXACTLY) {
            // Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            // Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            // Be whatever you want
            width = desiredWidth;
        }

        // Measure height
        if (heightMode == MeasureSpec.EXACTLY) {
            // Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            // Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            // Be whatever you want
            height = desiredHeight;
        }

        // Set the measured dimensions
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Define sizes for each block based on the percentages of the total
        float width = getWidth();
        float height = getHeight();

        // Draw Total Space (outer rectangle)
        paint.setColor(Color.LTGRAY);
        canvas.drawRect(0, 0, width, height, paint);

        // Draw Used Space (inside Total Space)
        paint.setColor(Color.BLUE);
        float usedHeight = (usedSpace / totalSpace) * height;
        canvas.drawRect(0, 0, width, usedHeight, paint);

        // Draw Media Space (inside Used Space)
        paint.setColor(Color.GREEN);
        float mediaHeight = (mediaSpace / usedSpace) * usedHeight;
        canvas.drawRect(0, 0, width, mediaHeight, paint);

        // Draw Synced Media Space (inside Media Space)
        paint.setColor(Color.RED);
        float syncedMediaHeight = (syncedMediaSpace / mediaSpace) * mediaHeight;
        canvas.drawRect(0, 0, width, syncedMediaHeight, paint);

        // Optional: Add text labels for clarity
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        canvas.drawText("Total Space", 20, height / 2, paint);
        canvas.drawText("Used Space", 20, usedHeight / 2, paint);
        canvas.drawText("Media", 20, mediaHeight / 2, paint);
        canvas.drawText("Synced Media", 20, syncedMediaHeight / 2, paint);
    }
}
