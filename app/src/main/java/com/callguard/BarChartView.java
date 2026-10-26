package com.callguard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Minimal bar chart drawn with the framework canvas: grid, rounded bars,
 * values above bars and multi-line labels below. No chart libraries.
 */
public final class BarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int[] values = new int[0];
    private String[] labels = new String[0];
    private boolean showNumbers = true;

    private final float density;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        barPaint.setColor(context.getColor(R.color.chart_bar));
        gridPaint.setColor(context.getColor(R.color.chart_grid));
        gridPaint.setStrokeWidth(Math.max(1f, density));
        textPaint.setColor(context.getColor(R.color.text_secondary));
    }

    public void setData(int[] values, String[] labels, boolean showNumbers) {
        this.values = values == null ? new int[0] : values;
        this.labels = labels == null ? new String[0] : labels;
        this.showNumbers = showNumbers;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = values.length;
        if (count == 0) {
            return;
        }

        float left = dp(30);
        float right = getWidth() - dp(4);
        float top = dp(showNumbers ? 16 : 6);
        float labelSpace = labels.length > 0 ? dp(26) : dp(6);
        float bottom = getHeight() - labelSpace;
        if (bottom <= top) {
            return;
        }

        int max = 0;
        for (int value : values) {
            max = Math.max(max, value);
        }
        int step = niceStep(max);
        int axisMax = step * 4;

        // Grid and Y labels.
        textPaint.setTextSize(dp(9));
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int index = 0; index <= 4; index++) {
            float y = bottom - (bottom - top) * index / 4f;
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(String.valueOf(step * index), left - dp(6), y + dp(3), textPaint);
        }

        float slot = (right - left) / count;
        float barWidth = Math.min(dp(26), slot * 0.56f);

        for (int index = 0; index < count; index++) {
            float center = left + slot * index + slot / 2f;
            float height = (bottom - top) * values[index] / (float) axisMax;
            if (height > 0) {
                RectF rect = new RectF(center - barWidth / 2f, bottom - height,
                        center + barWidth / 2f, bottom);
                canvas.drawRoundRect(rect, dp(5), dp(5), barPaint);
            }

            if (showNumbers && values[index] > 0) {
                textPaint.setTextSize(dp(10));
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(String.valueOf(values[index]), center,
                        bottom - height - dp(5), textPaint);
            }

            // The caller decides which labels to show; null means "skip".
            if (labels.length > index && labels[index] != null) {
                textPaint.setTextSize(dp(9));
                textPaint.setTextAlign(Paint.Align.CENTER);
                drawLabel(canvas, labels[index], center, bottom + dp(12));
            }
        }
    }

    private void drawLabel(Canvas canvas, String label, float center, float y) {
        String[] parts = label.split("\n");
        float offset = 0;
        for (String part : parts) {
            canvas.drawText(part, center, y + offset, textPaint);
            offset += dp(11);
        }
    }

    /** Rounds the axis step up to 1, 2, 5, 10, 20, 50 … so grid values stay readable. */
    private static int niceStep(int max) {
        int raw = (int) Math.ceil(max / 4.0);
        if (raw <= 1) {
            return 1;
        }
        if (raw <= 2) {
            return 2;
        }
        if (raw <= 5) {
            return 5;
        }
        int step = 10;
        while (step < raw) {
            step *= 10;
        }
        return step;
    }

    private float dp(float value) {
        return value * density;
    }
}
