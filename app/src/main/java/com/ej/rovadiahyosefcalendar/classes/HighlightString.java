package com.ej.rovadiahyosefcalendar.classes;

import androidx.annotation.NonNull;

import java.util.Objects;

public class HighlightString {

    private String string = "";
    private boolean shouldBeHighlighted = false;

    public HighlightString(String s) {
        setString(s);
    }

    public boolean shouldBeHighlighted() {
        return shouldBeHighlighted;
    }

    public HighlightString setShouldBeHighlighted(boolean shouldBeHighlighted) {
        this.shouldBeHighlighted = shouldBeHighlighted;
        return this;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighlightString that = (HighlightString) o;
        return shouldBeHighlighted == that.shouldBeHighlighted && Objects.equals(string, that.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string, shouldBeHighlighted);
    }

    @NonNull
    @Override
    public String toString() {
        return string;
    }
}
