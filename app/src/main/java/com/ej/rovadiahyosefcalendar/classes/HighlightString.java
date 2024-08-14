package com.ej.rovadiahyosefcalendar.classes;

import androidx.annotation.NonNull;

import java.util.Objects;

public class HighlightString {

    private String string = "";
    private boolean shouldBeHighlighted = false;
    private boolean isCategory = false;

    public HighlightString(String s) {
        setString(s);
    }

    public boolean shouldBeHighlighted() {
        return shouldBeHighlighted;
    }

    public boolean isCategory() {
        return isCategory;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighlightString that = (HighlightString) o;
        return shouldBeHighlighted == that.shouldBeHighlighted && isCategory == that.isCategory && Objects.equals(string, that.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string, shouldBeHighlighted, isCategory);
    }

    @NonNull
    @Override
    public String toString() {
        return string;
    }
}
