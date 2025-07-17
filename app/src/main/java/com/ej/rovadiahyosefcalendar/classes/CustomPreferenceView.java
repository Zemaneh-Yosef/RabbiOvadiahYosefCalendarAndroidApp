package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.github.tttt55.materialyoupreferences.preferences.MaterialPreference;

public class CustomPreferenceView extends MaterialPreference {

    private boolean isDimmed = false;

    public CustomPreferenceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomPreferenceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //setLayoutResource(R.layout.preference_custom); // Your custom layout with CardView
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

        holder.itemView.setTextDirection(View.TEXT_DIRECTION_LTR);
        // Apply dim effect
        holder.itemView.setAlpha(isDimmed ? 0.4f : 1.0f);

        // Optional: still clickable
        holder.itemView.setEnabled(true);
        holder.itemView.setClickable(true);
    }
}
