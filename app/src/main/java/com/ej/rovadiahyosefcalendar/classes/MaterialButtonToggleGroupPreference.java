package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class MaterialButtonToggleGroupPreference extends ListPreference {

    Button button1;
    Button button2;
    Button button3;

    boolean isBinding = false;

    public MaterialButtonToggleGroupPreference(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public MaterialButtonToggleGroupPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public MaterialButtonToggleGroupPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MaterialButtonToggleGroupPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.material_button_toggle_group_preference);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        isBinding = true;
        super.onBindViewHolder(holder);
        MaterialButtonToggleGroup toggleButton = (MaterialButtonToggleGroup) holder.findViewById(R.id.toggleButton);
        button1 = (Button) holder.findViewById(R.id.button1);
        button2 = (Button) holder.findViewById(R.id.button2);
        button3 = (Button) holder.findViewById(R.id.button3);
        button1.setText(getEntries()[0].toString());
        button1.setOnClickListener(l -> toggleButton.check(R.id.button1));
        button2.setText(getEntries()[1].toString());
        button2.setOnClickListener(l -> toggleButton.check(R.id.button2));
        button3.setText(getEntries()[2].toString());
        button3.setOnClickListener(l -> toggleButton.check(R.id.button3));
        toggleButton.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isBinding) {
                return;
            }
            if (getOnPreferenceClickListener() != null) {
                getOnPreferenceClickListener().onPreferenceClick(this);
            }
            if (!isChecked) {
                return;
            }
            group.post(() -> {
                if (checkedId == R.id.button1) {
                    setValue(getEntryValues()[0].toString());
                } else if (checkedId == R.id.button2) {
                    setValue(getEntryValues()[1].toString());
                } else {
                    setValue(getEntryValues()[2].toString());
                }
            });
        });
        if (getPersistedString(getKey()).equals(getEntryValues()[0].toString())) {
            toggleButton.check(R.id.button1);
        } else if (getPersistedString(getKey()).equals(getEntryValues()[1].toString())) {
            toggleButton.check(R.id.button2);
        } else if (getPersistedString(getKey()).equals(getEntryValues()[2].toString())) {
            toggleButton.check(R.id.button3);
        } else {// this could happen in rare cases where the string is not the same as the values. For example, the app's language has changed.
            toggleButton.clearChecked();
        }
        isBinding = false;
    }
}
