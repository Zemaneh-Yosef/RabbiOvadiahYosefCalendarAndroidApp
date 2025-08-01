package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;

import java.util.HashMap;
import java.util.Map;

public class SliderPreference extends Preference {

  private static final Map<EditText, SliderPreference> cache = new HashMap<>();
  private static final int NO_ALERT = -1;

  private MaterialCheckBox enabled;
  private Slider slider;
  private EditText currentValueDisplay;

  private int defaultValue;
  private int progress;

  private final CurrentValueTextWatcher currentValueTextWatcher;

  private final OnSliderListener sliderListener;

  private final View.OnClickListener enabledClickListener;

  public SliderPreference(Context context) {
    this(context, null, 0);
  }

  public SliderPreference(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SliderPreference(Context context, @Nullable AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setLayoutResource(R.layout.preference_slider);

    currentValueTextWatcher = new CurrentValueTextWatcher();
    sliderListener = new OnSliderListener();

    enabledClickListener = new EnabledClickListener();
  }

  @Override
  public void onDetached() {
    cleanup();

    cache.clear();// Someone has to clear the cache...

    super.onDetached();
  }

  @Override
  public void onDependencyChanged(@NonNull Preference dependency, boolean disableDependent) {

    super.onDependencyChanged(dependency, disableDependent);

    if (isViewReady()) {
      enabled.setEnabled(!disableDependent);
      enableControls(!disableDependent);
    }
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    final Parcelable superState = super.onSaveInstanceState();

    // Check whether this Preference is persistent (continually saved)
    if (isPersistent()) {
      // No need to save instance state since it's persistent, use superclass state
      return superState;
    }

    // Create instance of custom BaseSavedState
    final SavedState myState = new SavedState(superState);
    // Set the state's value with the class member that holds current setting value
    myState.value = progress;

    return myState;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {

    // Check whether we saved the state in onSaveInstanceState
    if (state == null || !state.getClass().equals(SavedState.class)) {
      // Didn't save the state, so call superclass
      super.onRestoreInstanceState(state);
      return;
    }

    // Cast state to custom BaseSavedState and pass to superclass
    SavedState myState = (SavedState) state;
    super.onRestoreInstanceState(myState.getSuperState());

    // Set this Preference's widget to reflect the restored state
    progress = myState.value;
    initView();
  }

  @Override
  public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
    cleanup();

    super.onBindViewHolder(holder);

    slider = (Slider) holder.findViewById(R.id.seekbar);
    slider.setValue(progress);

    currentValueDisplay = (EditText) holder.findViewById(R.id.value);
    enabled = (MaterialCheckBox) holder.findViewById(R.id.enable);

    updateCache();

    // Forces everything to get setup correctly.
    initView();

    // Add the listeners now, so we don't get anything while initializing.
    enabled.setOnClickListener(enabledClickListener);
    slider.addOnSliderTouchListener(sliderListener);
    slider.addOnChangeListener(sliderListener);
    slider.setLabelFormatter(v -> String.valueOf((int)v));
    currentValueDisplay.addTextChangedListener(currentValueTextWatcher);
  }

  private void updateCache() {
    final SliderPreference other = cache.remove(currentValueDisplay);
    if ((other != null) && (other != this)) {
      other.cleanup();
    }

    cache.put(currentValueDisplay, this);
  }

  private void cleanup() {
    if (slider != null) {
      slider.removeOnSliderTouchListener(sliderListener);
    }

    if (enabled != null) {
      enabled.setOnClickListener(null);
    }

    if (currentValueDisplay != null) {
      currentValueDisplay.removeTextChangedListener(currentValueTextWatcher);
    }
  }

  @Override
  protected void onPrepareForRemoval() {
    cleanup();
    super.onPrepareForRemoval();
  }

  @Override
  protected void onSetInitialValue(@Nullable Object defaultValue) {
    setValue(getPersistedInt(progress));
    super.onSetInitialValue(defaultValue);
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    defaultValue = a.getInt(index, 0);
    return defaultValue;
  }

  private void initView() {
    final boolean thisAlertIsEnabled = progress >= 0;
    enabled.setChecked(thisAlertIsEnabled);

    enableControls(thisAlertIsEnabled);
    updateValueDisplay(true);
  }

  private void enableControls(boolean isEnabled) {

    // Do NOT try to call setEnabled() on 'this' -- since we call this method from onBindViewHolder() and other times
    // while performing a layout, it will cause an exception.
    slider.setEnabled(isEnabled);
    currentValueDisplay.setEnabled(isEnabled);
  }

  private void setValue(int value) {
    if (value != progress) {

      persistInt(value);
      progress = value;

      // The onSetInitialValue gets called before onBindView.
      // NOTE: The 'best practice' says you should notify here, but... we're OK. We don't need to do anything until
      //  all the preferences are set.
//      if ( isViewReady() ) {
//        notifyChanged();
//      }
    }
  }

  private void updateValueDisplay(boolean initialUpdate) {

    currentValueDisplay.removeTextChangedListener(currentValueTextWatcher);

    if (progress >= 0) {

      // Don't do anything we don't have to do. (Note if the value is blank, it won't parse to an int, and we'll blow up...)
      int alertValue = getAlertValue();

      if (progress != alertValue) {
        currentValueDisplay.setText(String.valueOf(progress));
      }
    } else {
      currentValueDisplay.setText("");
    }

    if (!initialUpdate) {
      currentValueDisplay.addTextChangedListener(currentValueTextWatcher);
    }
  }

  private void updateSeekBar() {
    slider.setValue(progress);
  }

  private boolean isViewReady() {
    // Will be null when we're setting up.
    return currentValueDisplay != null;
  }

  private int getAlertValue() {
    final String value = currentValueDisplay.getText().toString();
    return (value.isEmpty() ? NO_ALERT : Integer.parseInt(value));
  }

  /**
   * Listen for the user to slide the seek bar, and make sure the text value is updated.
   */
  private class OnSliderListener implements Slider.OnSliderTouchListener, Slider.OnChangeListener {

    boolean isTracking = false;

    @Override
    public void onStartTrackingTouch(@NonNull Slider slider) {
      isTracking = true;
    }

    @Override
    public void onStopTrackingTouch(@NonNull Slider slider) {
      isTracking = false;
    }

    @Override
    public void onValueChange(@NonNull Slider slider, float progress, boolean fromUser) {
      if (!fromUser) {
        return;
      }

      setValue((int) progress);
      updateValueDisplay(false);
    }
  }

  /**
   * Listen for the user to enter a value in the text box, and make sure the seek bar is updated.
   */
  private class CurrentValueTextWatcher implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      if (!sliderListener.isTracking) {
        slider.removeOnChangeListener(sliderListener);
      }
    }


    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
      if (!sliderListener.isTracking) {
        int alertTime = getAlertValue();
        setValue(alertTime);
        updateSeekBar();
        slider.addOnSliderTouchListener(sliderListener);
      }
    }
  }

  /**
   * Save/restore the data. See <a href="https://developer.android.com/guide/topics/ui/settings.html">here</a>
   */
  private static class SavedState extends BaseSavedState {

    int value;

    SavedState(Parcelable superState) {
      super(superState);
    }

    SavedState(Parcel source) {
      super(source);
      // Get the current preference's value
      value = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      // Write the preference's value
      dest.writeInt(value);
    }

    // Standard creator object using an instance of this class
    public static final Creator<SavedState> CREATOR = new Creator<>() {

      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }

  private class EnabledClickListener implements View.OnClickListener {

    @Override
    public void onClick(final View v) {
      // Reflects the new value
      final boolean isThisAlertEnabled = enabled.isChecked();
      enableControls(isThisAlertEnabled);

      int newValue;
      if (isThisAlertEnabled) {
        newValue = getAlertValue();
        if (newValue == NO_ALERT) {
          newValue = defaultValue;
        }
      } else {
        newValue = NO_ALERT;
      }

      setValue(newValue);
      updateValueDisplay(false);
      updateSeekBar();
    }
  }
}
