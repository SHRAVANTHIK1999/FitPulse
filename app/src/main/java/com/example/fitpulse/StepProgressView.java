package com.example.fitpulse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

/**
 * Circular progress ring for daily steps.
 * - Draws a background track, a progress arc, and a center % label.
 * - Animates from the previous fraction to the new one when setSteps() is called.
 * - Updates content description for accessibility (TalkBack).
 */
public class StepProgressView extends View {

    // Paints for track, progress arc, and text
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint       = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Raw and inset drawing bounds
    private final RectF rawBounds  = new RectF();
    private final RectF drawBounds = new RectF();

    // Step values (max is the goal)
    private int maxSteps = 10000;
    private int currentSteps = 0;

    // Colors and stroke thickness
    private int trackColor    = Color.parseColor("#DADCE0");
    private int progressColor = Color.parseColor("#3F51B5");
    private int textColor     = Color.DKGRAY;
    private float strokePx    = 30f;

    // Current animated progress (0..1) and animator
    private float animatedFraction = 0f;
    private ValueAnimator animator;

    public StepProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /** Initialize paints, text, and accessibility. */
    private void init() {
        backgroundPaint.setColor(trackColor);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokePx);

        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokePx);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(50f);

        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /**
     * Update steps and goal, then animate to the new fraction.
     * @param currentSteps steps completed (>= 0)
     * @param maxSteps goal (>= 1)
     */
    public void setSteps(int currentSteps, int maxSteps) {
        int oldSteps = this.currentSteps;
        this.currentSteps = Math.max(0, currentSteps);
        this.maxSteps = Math.max(1, maxSteps);

        float start = clamp01(oldSteps / (float) this.maxSteps);
        float end   = clamp01(this.currentSteps / (float) this.maxSteps);
        startProgressAnimation(start, end);

        int percent = Math.min(100, Math.round(100f * end));
        setContentDescription(percent + "% of daily goal, " + this.currentSteps + " steps");
        if (isAccessibilityEnabled()) announceForAccessibility(getContentDescription());
    }

    // Simple style setters
    public void setTrackColor(int color) { trackColor = color; backgroundPaint.setColor(color); invalidate(); }
    public void setProgressColor(int color) { progressColor = color; progressPaint.setColor(color); invalidate(); }
    public void setTextColor(int color) { textColor = color; textPaint.setColor(color); invalidate(); }
    public void setStrokeWidth(float px) {
        strokePx = px;
        backgroundPaint.setStrokeWidth(px);
        progressPaint.setStrokeWidth(px);
        invalidate();
    }

    /** Recompute bounds and dynamic sizes when view size changes. */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int left   = getPaddingLeft();
        int top    = getPaddingTop();
        int right  = w - getPaddingRight();
        int bottom = h - getPaddingBottom();

        int size = Math.min(right - left, bottom - top);
        int cx = left + (right - left) / 2;
        int cy = top + (bottom - top) / 2;
        float radius = size / 2f;

        // Scale stroke and text with radius for better look on different sizes
        float dynamicStroke = Math.max(20f, radius * 0.14f);
        setStrokeWidth(dynamicStroke);

        rawBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        textPaint.setTextSize(Math.max(42f, radius * 0.38f));
    }

    /** Draw track circle, progress arc, and percentage label. */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Inset so stroke is fully inside the bounds
        drawBounds.set(rawBounds);
        float inset = strokePx / 2f;
        drawBounds.inset(inset, inset);

        // Background ring
        float radius = drawBounds.width() / 2f;
        canvas.drawCircle(drawBounds.centerX(), drawBounds.centerY(), radius, backgroundPaint);

        // Progress arc (starts at top, -90 degrees)
        float sweepAngle = 360f * animatedFraction;
        canvas.drawArc(drawBounds, -90f, sweepAngle, false, progressPaint);

        // Center text (%), vertically centered
        int percent = Math.min(100, Math.round(100f * animatedFraction));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = drawBounds.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(percent + "%", drawBounds.centerX(), textY, textPaint);
    }

    /** True if any accessibility service is enabled (for announcements). */
    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled();
    }

    /** Clamp value to [0, 1]. */
    private float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    /** Smoothly animate from start fraction to end fraction. */
    private void startProgressAnimation(float start, float end) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(600);
        animator.addUpdateListener(a -> {
            animatedFraction = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }
}
