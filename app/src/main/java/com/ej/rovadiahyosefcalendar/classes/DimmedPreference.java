package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class DimmedPreference extends Preference {

    private boolean isDimmed = false;

    public DimmedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DimmedPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDimmed(boolean dimmed) {
        this.isDimmed = dimmed;
        notifyChanged(); // triggers onBindViewHolder
    }

    public boolean isDimmed() {
        return isDimmed;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (isDimmed) {
            holder.itemView.setAlpha(0.4f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        // Optional: still clickable
        holder.itemView.setEnabled(true);
        holder.itemView.setClickable(true);
    }
}
