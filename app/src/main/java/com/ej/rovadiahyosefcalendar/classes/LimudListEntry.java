package com.ej.rovadiahyosefcalendar.classes;

public class LimudListEntry {

    private String limudTitle;
    private String description;
    private String source;
    private boolean hasSource;

    public LimudListEntry(String limudTitle) {
        this.limudTitle = limudTitle;
    }

    public LimudListEntry(String limudTitle, String source) {
        this.limudTitle = limudTitle;
        this.source = source;
        this.hasSource = true;
    }

    public LimudListEntry(String limudTitle, String description, String source) {
        this.limudTitle = limudTitle;
        this.description = description;
        this.source = source;
        this.hasSource = true;
    }

    public String getLimudTitle() {
        return limudTitle;
    }

    public void setLimudTitle(String limudTitle) {
        this.limudTitle = limudTitle;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean hasSource() {
        return hasSource;
    }

    public void setHasSource(boolean hasSource) {
        this.hasSource = hasSource;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
