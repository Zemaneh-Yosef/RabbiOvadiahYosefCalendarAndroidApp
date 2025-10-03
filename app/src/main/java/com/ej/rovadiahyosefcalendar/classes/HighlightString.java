package com.ej.rovadiahyosefcalendar.classes;

import androidx.annotation.NonNull;

import java.util.Objects;

public class HighlightString {

    private String string = "";
    private String summary;
    private boolean shouldBeHighlighted = false;
    private boolean isCategory = false;
    private boolean isInfo = false;
    private boolean needsMinyan = false;

    public HighlightString(String s) {
        setString(s);
    }

    public HighlightString(String s, String summary) {
        setString(s);
        setSummary(summary);
        setInfo(true);
    }

    public HighlightString setShouldBeHighlighted(boolean shouldBeHighlighted) {
        this.shouldBeHighlighted = shouldBeHighlighted;
        return this;
    }

    public void setString(String string) {
        this.string = string;
    }

    public HighlightString setCategory(boolean category) {
        this.isCategory = category;
        return this;
    }

    public HighlightString setInfo(boolean info) {
        this.isInfo = info;
        return this;
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
    public boolean shouldBeHighlighted() {
        return shouldBeHighlighted;
    }
    public boolean isCategory() {
        return isCategory;
    }
    public boolean isInfo() {
        return isInfo;
    }
    public String getSummary() {
        return summary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighlightString that = (HighlightString) o;
        return shouldBeHighlighted == that.shouldBeHighlighted && isCategory == that.isCategory && Objects.equals(string, that.string) && isInfo == that.isInfo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(string, shouldBeHighlighted, isCategory, isInfo);
    }

    @NonNull
    @Override
    public String toString() {
        return string;
    }
}
