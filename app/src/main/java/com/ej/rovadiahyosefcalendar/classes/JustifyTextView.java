package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.Nullable;

/**
 * Custom class to support Justifying Text. Created by chatGPT and a bit of tinkering.
 * The things I do for a Rabbi Fayazi (My Rosh Yeshiva) request... I'm just happy it's over...
 */
public class JustifyTextView extends androidx.appcompat.widget.AppCompatTextView {

    private boolean justify;

    public JustifyTextView(Context context) {
        super(context);
    }

    public JustifyTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JustifyTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (justify && getGravity() != Gravity.CENTER) {
            TextPaint textPaint = getPaint();
            textPaint.setColor(getCurrentTextColor());

            String text = getText().toString();
            int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            float y = getPaddingTop() * 2.5f; // Start from the top padding

            for (String paragraph : text.split("\n")) {
                y = drawJustifiedParagraph(canvas, paragraph, viewWidth, y, textPaint);
            }
        } else {
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (justify && getGravity() != Gravity.CENTER) {
            int viewWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
            TextPaint textPaint = getPaint();
            String text = getText().toString();

            float y = getPaddingTop() * 2.5f; // Account for extra top padding
            for (String paragraph : text.split("\n")) {
                y = measureParagraphHeight(paragraph, viewWidth, y, textPaint);
            }

            // Set the measured height with additional bottom padding
            int measuredHeight = (int) (y + getPaddingBottom());
            setMeasuredDimension(getMeasuredWidth(), measuredHeight);
        }
    }

    private float measureParagraphHeight(String paragraph, int viewWidth, float y, TextPaint textPaint) {
        if (paragraph.trim().isEmpty()) {
            return y + getLineHeight();
        }

        String[] words = paragraph.split("\\s+");
        float lineWidth = 0;

        for (String word : words) {
            float wordWidth = textPaint.measureText(word + " ");
            if (lineWidth + wordWidth > viewWidth) {
                y += getLineHeight();
                lineWidth = 0;
            }
            lineWidth += wordWidth;
        }

        return y + getLineHeight(); // Account for the last line
    }

    private float drawJustifiedParagraph(Canvas canvas, String paragraph, int viewWidth, float y, TextPaint textPaint) {
        if (paragraph.trim().isEmpty()) {
            return y + getLineHeight(); // Add empty line space
        }

        String[] words = paragraph.split("\\s+");
        StringBuilder line = new StringBuilder();
        float lineWidth = 0;

        for (String word : words) {
            float wordWidth = textPaint.measureText(word + " ");
            if (lineWidth + wordWidth > viewWidth) {
                drawJustifiedLine(canvas, line.toString().trim(), viewWidth, y, textPaint, isRtl(line.toString()));
                y += getLineHeight();
                line = new StringBuilder();
                lineWidth = 0;
            }
            line.append(word).append(" ");
            lineWidth += wordWidth;
        }

        // Draw the last line, aligned based on RTL
        drawAlignedLine(canvas, line.toString().trim(), viewWidth, y, textPaint, isRtl(line.toString()));
        return y + getLineHeight();
    }

    private void drawJustifiedLine(Canvas canvas, String line, int viewWidth, float y, TextPaint textPaint, boolean isRtl) {
        String[] words = line.split("\\s+");
        if (words.length == 1) {
            // Only one word; align to start (left or right depending on RTL)
            drawAlignedLine(canvas, line, viewWidth, y, textPaint, isRtl);
            return;
        }

        float totalWordWidth = 0;
        for (String word : words) {
            totalWordWidth += textPaint.measureText(word);
        }

        float totalSpaceWidth = viewWidth - totalWordWidth;
        float spaceWidth = totalSpaceWidth / (words.length - 1);

        float x = isRtl ? viewWidth + getPaddingLeft() : getPaddingLeft(); // Adjust starting position
        for (String word : words) {
            float wordWidth = textPaint.measureText(word);
            if (isRtl) {
                x -= wordWidth;
                canvas.drawText(word, x, y, textPaint);
                x -= spaceWidth;
            } else {
                canvas.drawText(word, x, y, textPaint);
                x += wordWidth + spaceWidth;
            }
        }
    }

    private void drawAlignedLine(Canvas canvas, String line, int viewWidth, float y, TextPaint textPaint, boolean isRtl) {
        float x = isRtl
                ? viewWidth - textPaint.measureText(line) + getPaddingLeft()
                : getPaddingLeft();
        canvas.drawText(line, x, y, textPaint);
    }

    private boolean isRtl(String text) {
        if (text.isEmpty()) {
            return false;
        }
        char firstChar = text.charAt(0);
        int directionality = Character.getDirectionality(firstChar);
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    public void setJustify(boolean justify) {
        this.justify = justify;
    }
}


