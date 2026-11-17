package com.ej.rovadiahyosefcalendar.classes;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.ej.rovadiahyosefcalendar.R;

/**
 * Draws two seamlessly tiling cloud layers over the molad background (molad_sky_720) and drifts them to the right, forever:
 * the clouds near the bottom (faster) and the wispy band near the top (slower, for a sense of depth). A small, fixed picture of
 * the moon is drawn last so the wisps always pass behind the moon and never over it.
 * <p>
 * Each cloud bitmap is exactly one loop wide, so when the animation wraps around the picture is identical and there is no visible
 * jump. Everything is positioned with the same "centerCrop" math as the ImageView underneath, so the layers stay lined up with the
 * sky on every screen size.
 * <p>
 * The animation only runs while the view is attached and visible, and it is skipped when the system has animations turned off.
 */
public class DriftingCloudsView extends View {

    /** The size of the background image (molad_sky_720) that these layers sit on. */
    private static final int BACKGROUND_WIDTH = 720;
    private static final int BACKGROUND_HEIGHT = 1280;
    /** One full cycle of the animation. Each layer finishes a whole number of loops in a cycle, so everything wraps together. */
    private static final long CYCLE_DURATION_MILLIS = 240_000;
    /** Where (in background pixels) the top left of the moon picture belongs. */
    private static final int MOON_LEFT = 405;
    private static final int MOON_TOP = 295;

    /** One drifting, tiling layer. */
    private static final class Layer {
        final Bitmap bitmap;
        /** The row of the background image where the top of the bitmap belongs. */
        final int top;
        /** How many times the bitmap loops in one cycle. Higher is faster. */
        final int loopsPerCycle;

        Layer(Bitmap bitmap, int top, int loopsPerCycle) {
            this.bitmap = bitmap;
            this.top = top;
            this.loopsPerCycle = loopsPerCycle;
        }
    }

    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private Layer[] mLayers = new Layer[0];
    private Bitmap mMoon;
    private ValueAnimator mAnimator;
    /** 0 to 1, how far through one cycle the animation is. */
    private float mCycle = 0f;

    public DriftingCloudsView(Context context) {
        super(context);
        init();
    }

    public DriftingCloudsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DriftingCloudsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Bitmap wisps = decode(R.drawable.molad_wisps_640);
        Bitmap clouds = decode(R.drawable.molad_clouds_600);
        mMoon = decode(R.drawable.molad_moon_260);
        // wisps first so the lower clouds are drawn over them; the wisps drift at half the speed of the clouds
        if (wisps != null && clouds != null) {
            mLayers = new Layer[]{new Layer(wisps, 30, 1), new Layer(clouds, 540, 2)};
        }
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private Bitmap decode(int resId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(getResources(), resId, options);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mLayers.length == 0 || !ValueAnimator.areAnimatorsEnabled()) {
            return; // nothing to draw, or the user turned animations off: leave the clouds still
        }
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(CYCLE_DURATION_MILLIS);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.addUpdateListener(animation -> {
            mCycle = (float) animation.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    /** Stops the animation (and the battery use) while the screen or the activity is not visible. */
    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (mAnimator != null) {
            if (isVisible) {
                mAnimator.resume();
            } else {
                mAnimator.pause();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mLayers.length == 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        // the same scaling and centering that ImageView's centerCrop does for the background
        float scale = Math.max(getWidth() / (float) BACKGROUND_WIDTH, getHeight() / (float) BACKGROUND_HEIGHT);
        float left = (getWidth() - BACKGROUND_WIDTH * scale) / 2f;
        float top = (getHeight() - BACKGROUND_HEIGHT * scale) / 2f;
        float visibleStart = -left / scale;         // background x at the left edge of the screen
        float visibleEnd = (getWidth() - left) / scale;

        canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        for (Layer layer : mLayers) {
            int tileWidth = layer.bitmap.getWidth();
            float phase = (mCycle * layer.loopsPerCycle) % 1f;
            float shift = phase * tileWidth;        // in background pixels, grows to the right
            // draw as many copies of the tile as it takes to cover the screen, starting from the one that holds visibleStart
            float x = shift + (float) Math.floor((visibleStart - shift) / tileWidth) * tileWidth;
            for (; x < visibleEnd; x += tileWidth) {
                canvas.drawBitmap(layer.bitmap, x, layer.top, mPaint);
            }
        }
        if (mMoon != null) {
            canvas.drawBitmap(mMoon, MOON_LEFT, MOON_TOP, mPaint); // fixed, on top, so the wisps pass behind the moon
        }
        canvas.restore();
    }
}
