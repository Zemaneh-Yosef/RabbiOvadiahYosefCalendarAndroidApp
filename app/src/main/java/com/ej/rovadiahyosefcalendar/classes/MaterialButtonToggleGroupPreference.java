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
        button2.setText(getEntries()[1].toString());
        button3.setText(getEntries()[2].toString());
        toggleButton.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isBinding) {
                return;
            }
            if (getOnPreferenceClickListener() != null) {
                getOnPreferenceClickListener().onPreferenceClick(this);
            }
            if (isChecked) {
                group.post(() -> {
                    if (checkedId == R.id.button1) {
                        setValue(button1.getText().toString());
                    } else if (checkedId == R.id.button2) {
                        setValue(button2.getText().toString());
                    } else {
                        setValue(button3.getText().toString());
                    }
                });
            }
        });
        String firstButtonValue = button1.getText().toString();
        String secondButtonValue = button2.getText().toString();
        String thirdButtonValue = button3.getText().toString();
        if (getPersistedString(getKey()).equals(firstButtonValue)) {
            toggleButton.check(R.id.button1);
        } else if (getPersistedString(getKey()).equals(secondButtonValue)) {
            toggleButton.check(R.id.button2);
        } else if (getPersistedString(getKey()).equals(thirdButtonValue)) {
            toggleButton.check(R.id.button3);
        } else {// this could happen in rare cases where the string is not the same as the button text. For example, the app's language has changed.
            toggleButton.clearChecked();
        }
        isBinding = false;
    }
}
