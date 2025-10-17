package com.ej.rovadiahyosefcalendar.classes;

import android.text.Spannable;

import androidx.annotation.NonNull;

import java.util.Objects;

public class HighlightString {

    private String string = "";
    private String summary;
    private StringType type;
    private boolean needsMinyan = false;
    private Spannable spannableString;

    public enum StringType {
        STANDARD,
        HIGHLIGHT,
        CATEGORY,
        INFO,
        INSTRUCTION,
        PARAGRAPH
    }

    public HighlightString(String s) {
        setString(s);
    }

    public HighlightString(Spannable s) {
        setSpannableString(s);
    }

    private void setSpannableString(Spannable s) {
        this.spannableString = s;
    }

    public Spannable getSpannableString() {
        return spannableString;
    }

    public HighlightString(String s, String summary) {
        setString(s);
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

    public void setString(String string) {
        this.string = string;
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
    public boolean isSpannableString() { return spannableString != null; }

    public StringType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighlightString that = (HighlightString) o;
        return type == that.getType() && Objects.equals(string, that.string) && Objects.equals(spannableString, that.getSpannableString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(string, spannableString, summary, type);
    }

    @NonNull
    @Override
    public String toString() {
        return string;
    }
}
