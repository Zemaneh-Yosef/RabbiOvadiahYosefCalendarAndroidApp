package com.ej.rovadiahyosefcalendar.classes;

import androidx.annotation.NonNull;

import java.util.Objects;

public class HighlightString {

    private CharSequence content = "";
    private String summary;
    private StringType type;
    private boolean needsMinyan = false;
    /**
     * This isHighlighted used to be a type - however, I would like instructions to also be highlightable
     * */
    private boolean isHighlighted = false;
    private int bigWordsStart = 0;
    private ImageAttachment imageAttachment = ImageAttachment.NONE;

	public ImageAttachment getImageAttachment() {
		return imageAttachment;
	}

	public void setImageAttachment(ImageAttachment imageAttachment) {
		this.imageAttachment = imageAttachment;
	}

	public boolean isHighlighted() {
		return isHighlighted;
	}

	public void setHighlighted(boolean highlighted) {
		isHighlighted = highlighted;
	}

    public HighlightString setHighlightedChain(boolean highlighted) {
        isHighlighted = highlighted;
        return this;
    }

	public enum StringType {
        STANDARD,
        CATEGORY,
        INFO,
        INVERSE_INFO,
        INSTRUCTION
    }

    public enum ImageAttachment {
        NONE,
        MENORAH,
        COMPASS
    }

    public HighlightString(CharSequence s) {
        setContent(s);
    }

    public HighlightString(String s, String summary) {
        setContent(s);
        setSummary(summary);
        setType(StringType.INFO);
    }

    public HighlightString setTypeChain(StringType type) {
        this.type = type;
        return this;
    }
    public void setType(StringType type) {
        this.type = type;
    }

    public void setContent(CharSequence content) {
        this.content = content;
    }

    public void setBigWordsStart(int bigWordsStart) {
        this.bigWordsStart = bigWordsStart;
    }

    public HighlightString setBigWordsStartChain(int bigWordsStart) {
        this.bigWordsStart = bigWordsStart;
        return this;
    }

    public int getBigWordsStart() {
        return bigWordsStart;
    }

    public HighlightString setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public HighlightString setNeedsMinyan(boolean needsMinyan) {
        this.needsMinyan = needsMinyan;
        return this;
    }

    public boolean isNeedsMinyan() {
        return needsMinyan;
    }
    public String getSummary() {
        return summary;
    }

    public StringType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighlightString that = (HighlightString) o;
        return type == that.getType() && Objects.equals(content, that.content) && Objects.equals(summary, that.summary) && Objects.equals(imageAttachment, that.imageAttachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, summary, type, needsMinyan);
    }

    @NonNull
    @Override
    public String toString() {
        return content.toString();
    }

    public CharSequence getContent() {
        return content;
    }
}
