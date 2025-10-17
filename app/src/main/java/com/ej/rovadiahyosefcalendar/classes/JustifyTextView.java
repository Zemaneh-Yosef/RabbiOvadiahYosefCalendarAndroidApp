package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Custom TextView that uses a manual two-layout system to correctly render a
 * large first word with text wrapping for RTL layouts. It also supports text justification and respects gravity.
 */
public class JustifyTextView extends AppCompatTextView {

    private boolean isLargeFirstWord = false;
    private boolean isJustified = false;
    private final float firstWordScale = 1.7f;
    private TextPaint mTextPaint; // Reusable paint object
    public boolean wasClicked = false;

    public JustifyTextView(Context context) {
        super(context);
        init();
    }

    public JustifyTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public JustifyTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    }

    private Layout.Alignment getLayoutAlignment() {
        int gravity = getGravity();
        // Check for horizontal center gravity
        if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.CENTER_HORIZONTAL) {
            return Layout.Alignment.ALIGN_CENTER;
        }

        // For RTL languages like Hebrew, ALIGN_OPPOSITE creates a right-aligned layout.
        boolean isRtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        return isRtl ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mTextPaint.set(getPaint());

        if (!isLargeFirstWord) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int viewWidth = parentWidth - getPaddingLeft() - getPaddingRight();

        float totalHeight = 0;
        String[] paragraphs = getText().toString().split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                totalHeight += getLineHeight();
                continue;
            }

            Layout.Alignment alignment = getLayoutAlignment();
            boolean isCentered = alignment == Layout.Alignment.ALIGN_CENTER;

            String[] words = paragraph.trim().split("\\s+");
            // If the paragraph is centered or just one word, measure it with a simple StaticLayout.
            if (isCentered || words.length <= 1) {
                StaticLayout sl = new StaticLayout(paragraph, mTextPaint, viewWidth, alignment, getLineSpacingMultiplier(), getLineSpacingExtra(), true);
                totalHeight += sl.getHeight();
                continue;
            }

            // --- Measure complex paragraph with large first word ---
            String firstWord = words[0];
            String restOfText = paragraph.trim().substring(firstWord.length()).trim();

            TextPaint firstWordPaint = new TextPaint(mTextPaint);
            firstWordPaint.setTextSize(mTextPaint.getTextSize() * firstWordScale);
            float firstWordWidth = firstWordPaint.measureText(firstWord);
            Paint.FontMetrics firstWordMetrics = firstWordPaint.getFontMetrics();
            float firstWordHeight = firstWordMetrics.descent - firstWordMetrics.ascent;

            float space = mTextPaint.measureText(" ");
            int indentedWidth = viewWidth - (int) Math.ceil(firstWordWidth) - (int) space;
            if (indentedWidth < 0) indentedWidth = 0;

            StaticLayout restLayout = new StaticLayout(restOfText, mTextPaint, indentedWidth, getLayoutAlignment(), getLineSpacingMultiplier(), getLineSpacingExtra(), true);

            int linesBesideLargeWord = 0;
            for (int i = 0; i < restLayout.getLineCount(); i++) {
                if (restLayout.getLineBottom(i) <= firstWordHeight) {
                    linesBesideLargeWord++;
                } else {
                    break;
                }
            }

            float heightOfIndentedBlock = linesBesideLargeWord > 0 ? restLayout.getLineBottom(linesBesideLargeWord - 1) : 0;
            float combinedHeight = Math.max(firstWordHeight, heightOfIndentedBlock);

            if (restLayout.getLineCount() > linesBesideLargeWord) {
                int wrapLineStart = restLayout.getLineStart(linesBesideLargeWord);
                String wrappingText = restOfText.substring(wrapLineStart);
                StaticLayout wrappingLayout = new StaticLayout(wrappingText, mTextPaint, viewWidth, getLayoutAlignment(), getLineSpacingMultiplier(), getLineSpacingExtra(), true);
                totalHeight += combinedHeight + wrappingLayout.getHeight();
            } else {
                totalHeight += combinedHeight;
            }
        }
        setMeasuredDimension(parentWidth, Math.round(totalHeight) + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mTextPaint.set(getPaint());
        mTextPaint.setColor(getCurrentTextColor());

        boolean isRtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

        if (!isLargeFirstWord && !isJustified) {
            super.onDraw(canvas);
            return;
        }

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());

        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float currentY = 0;

        // Handle simple cases (no large first word)
        if (!isLargeFirstWord) {
            Layout.Alignment alignment = getLayoutAlignment();
            StaticLayout layout = new StaticLayout(getText(), mTextPaint, viewWidth, alignment, getLineSpacingMultiplier(), getLineSpacingExtra(), true);
            if (isJustified) {
                drawJustified(canvas, layout, viewWidth, 0, isRtl);
            } else {
                layout.draw(canvas);
            }
            canvas.restore();
            return;
        }

        // --- Handle complex drawing paragraph by paragraph ---
        String[] paragraphs = getText().toString().split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                currentY += getLineHeight();
                continue;
            }

            Layout.Alignment paraAlignment = getLayoutAlignment();
            boolean isParaCentered = paraAlignment == Layout.Alignment.ALIGN_CENTER;
            String[] words = paragraph.trim().split("\\s+");

            // CASE 1: Paragraph is centered OR has only one word. Draw it with a single StaticLayout.
            if (isParaCentered || words.length <= 1) {
                StaticLayout layout = new StaticLayout(paragraph, mTextPaint, viewWidth, paraAlignment, getLineSpacingMultiplier(), getLineSpacingExtra(), true);
                canvas.save();
                canvas.translate(0, currentY);
                layout.draw(canvas);
                canvas.restore();
                currentY += layout.getHeight();
                continue;
            }

            // CASE 2: Multi-word, non-centered paragraph with large first word logic.
            String firstWord = words[0];
            String restOfText = paragraph.trim().substring(firstWord.length()).trim();

            TextPaint firstWordPaint = new TextPaint(mTextPaint);
            firstWordPaint.setTextSize(mTextPaint.getTextSize() * firstWordScale);
            float firstWordWidth = firstWordPaint.measureText(firstWord);
            Paint.FontMetrics firstWordMetrics = firstWordPaint.getFontMetrics();
            float firstWordHeight = firstWordMetrics.descent - firstWordMetrics.ascent;

            // Draw the large first word, always right-aligned for RTL.
            canvas.drawText(firstWord, viewWidth - firstWordWidth, currentY - firstWordMetrics.ascent, firstWordPaint);

            float space = mTextPaint.measureText(" ");
            int indentedWidth = viewWidth - (int) Math.ceil(firstWordWidth) - (int) space;
            if (indentedWidth < 0) indentedWidth = 0;

            // This layout is only for measurement and finding the wrap point.
            StaticLayout fullRestLayout = new StaticLayout(restOfText, mTextPaint, indentedWidth, paraAlignment, getLineSpacingMultiplier(), getLineSpacingExtra(), true);

            int linesToDrawIndented = 0;
            for (int i = 0; i < fullRestLayout.getLineCount(); i++) {
                if (fullRestLayout.getLineBottom(i) <= firstWordHeight) {
                    linesToDrawIndented++;
                } else {
                    break;
                }
            }

            float indentedHeight = (linesToDrawIndented > 0) ? fullRestLayout.getLineBottom(linesToDrawIndented - 1) : 0;

            // Draw the indented text (beside the large word)
            if (linesToDrawIndented > 0) {
                int indentedTextEnd = fullRestLayout.getLineEnd(linesToDrawIndented - 1);
                String indentedText = restOfText.substring(0, indentedTextEnd);
                StaticLayout indentedLayout = new StaticLayout(indentedText, mTextPaint, indentedWidth, paraAlignment, getLineSpacingMultiplier(), getLineSpacingExtra(), true);

                canvas.save();
                canvas.translate(0, currentY);
                if (isJustified) {
                    drawJustified(canvas, indentedLayout, indentedWidth, 0, isRtl);
                } else {
                    indentedLayout.draw(canvas);
                }
                canvas.restore();
            }

            // Draw the wrapped text (below the large word)
            float heightOfWrappedPart = 0;
            if (fullRestLayout.getLineCount() > linesToDrawIndented) {
                int wrappedTextStart = fullRestLayout.getLineStart(linesToDrawIndented);
                String wrappedText = restOfText.substring(wrappedTextStart);
                StaticLayout wrappedLayout = new StaticLayout(wrappedText, mTextPaint, viewWidth, paraAlignment, getLineSpacingMultiplier(), getLineSpacingExtra(), true);

                canvas.save();
                canvas.translate(0, currentY + indentedHeight);
                if (isJustified) {
                    drawJustified(canvas, wrappedLayout, viewWidth, 0, isRtl);
                } else {
                    wrappedLayout.draw(canvas);
                }
                canvas.restore();
                heightOfWrappedPart = wrappedLayout.getHeight();
            }

            currentY += Math.max(firstWordHeight, indentedHeight) + heightOfWrappedPart;
        }
        canvas.restore();
    }

    /**
     * Draws the text from a StaticLayout, justifying it line by line using a unified, Bidi-safe, word-by-word drawing method.
     */
    private void drawJustified(Canvas canvas, StaticLayout layout, int width, float yOffset, boolean isRtl) {
        String text = layout.getText().toString();
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();

        for (int i = 0; i < layout.getLineCount(); i++) {
            int lineStart = layout.getLineStart(i);
            int lineEnd = layout.getLineEnd(i);
            String line = text.substring(lineStart, lineEnd);
            String trimmedLine = line.trim();

            float lineTop = layout.getLineTop(i) + yOffset;
            float lineBaseline = lineTop - fm.ascent;

            boolean isLastLineOfLayout = (i == layout.getLineCount() - 1);
            String[] words = trimmedLine.split("\\s+");

            // Do NOT justify the last line, empty lines, or lines with 1 word. Draw them normally.
            if (isLastLineOfLayout || line.endsWith("\n") || trimmedLine.isEmpty() || words.length <= 1) {
                float x = isRtl ? width - mTextPaint.measureText(trimmedLine) : 0;
                canvas.drawText(trimmedLine, x, lineBaseline, mTextPaint);
                continue;
            }

            // --- Justification by drawing word-by-word (Corrected Logic) ---
            float totalWordWidth = 0;
            for (String word : words) {
                totalWordWidth += mTextPaint.measureText(word);
            }

            float gapSpace = (words.length > 1) ? (width - totalWordWidth) / (words.length - 1) : 0;

            float currentX = isRtl ? width : 0;
            for (String word : words) {
                if (isRtl) {
                    float wordWidth = mTextPaint.measureText(word);
                    currentX -= wordWidth;
                    canvas.drawText(word, currentX, lineBaseline, mTextPaint);
                    currentX -= gapSpace;
                } else {
                    canvas.drawText(word, currentX, lineBaseline, mTextPaint);
                    currentX += mTextPaint.measureText(word) + gapSpace;
                }
            }
        }
    }


    private void invalidateLayout() {
        requestLayout();
        invalidate();
    }

    // --- Public Methods & Overrides ---
    public void setJustify(boolean justified) {
        if (this.isJustified != justified) {
            this.isJustified = justified;
            invalidateLayout();
        }
    }

    public void setLargeFirstWord(boolean largeFirstWord) {
        if (this.isLargeFirstWord != largeFirstWord) {
            this.isLargeFirstWord = largeFirstWord;
            invalidateLayout();
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        invalidateLayout();
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        invalidate();
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        invalidateLayout();
    }
}
