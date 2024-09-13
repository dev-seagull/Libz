package com.example.cso.UI;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class LiquidFillButton extends FrameLayout {
    private Paint paint;
    private float fillLevel = 0f;
    private Path circularPath;
    private int startColor = Color.parseColor("#A2EAA5");
    private int endColor = Color.parseColor("#22A627");
    private Shader.TileMode tileMode = Shader.TileMode.CLAMP;
    private ObjectAnimator fillAnimator;

    public LiquidFillButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        circularPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        paint.setShader(new LinearGradient(0, 0, w, h, startColor, endColor, tileMode));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radius = Math.min(getWidth(), getHeight()) / 2f;
        if (circularPath != null) {
            circularPath.reset();
            circularPath.addCircle(getWidth() / 2f, getHeight() / 2f, radius, Path.Direction.CW);
            canvas.clipPath(circularPath);

            float height = getHeight() * (1 - fillLevel);

            canvas.drawRect(0, height, getWidth(), getHeight(), paint);
        }
    }

    public void startFillAnimation() {
        fillAnimator = ObjectAnimator.ofFloat(this, "fillLevel", 0f, 1f);
        fillAnimator.setDuration(5000);
        fillAnimator.start();
        fillAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        fillAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        fillAnimator.start();
    }

    public void setFillLevel(float fillLevel) {
        this.fillLevel = fillLevel;
        invalidate();
    }

    public float getFillLevel() {
        return fillLevel;
    }

    public void endFillAnimation() {
        if (fillAnimator != null && fillAnimator.isRunning()) {
            fillAnimator.cancel();
        }
        setFillLevel(0f);
    }
}
