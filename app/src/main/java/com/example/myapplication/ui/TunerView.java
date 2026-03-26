package com.example.myapplication.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class TunerView extends View {

    private float pitch = 0f;
    private Paint needlePaint;
    private Paint textPaint;

    public TunerView(Context context) {
        super(context);
        needlePaint = new Paint();
        needlePaint.setColor(Color.RED);
        needlePaint.setStrokeWidth(8);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(60);
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // draw simple needle
        float angle = (pitch - 440) / 100 * 90; // demo
        canvas.drawLine(w/2, h/2, w/2 + (float)Math.sin(Math.toRadians(angle))*200,
                h/2 - (float)Math.cos(Math.toRadians(angle))*200, needlePaint);

        // draw pitch value
        canvas.drawText(String.format("%.2f Hz", pitch), w/4f, h - 50, textPaint);
    }
}