package com.ej.rovadiahyosefcalendar.classes;

import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public String toString() {
        return string;
    }
}
