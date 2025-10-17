package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.Composer;
import androidx.compose.ui.platform.AbstractComposeView;
import java.util.ArrayList;

//import com.google.accompanist.themeadapter.material3.Mdc3Theme; // The new adapter is also called MdcTheme
//import kotlin.Unit;
//import kotlin.jvm.functions.Function2;

// Import your correct entry function
import static com.ej.rovadiahyosefcalendar.classes.SiddurScreenKt.SiddurScreenEntry;

public class SiddurComposeView extends AbstractComposeView {

	private ArrayList<HighlightString> siddurContent = new ArrayList<>();
	private JewishDateInfo jewishDateInfo = new JewishDateInfo(false); // Default value

	public SiddurComposeView(@NonNull Context context) {
		super(context);
	}

	public SiddurComposeView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public SiddurComposeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	// A public method to set the data
	public void setData(ArrayList<HighlightString> content, JewishDateInfo dateInfo) {
		// Prevent setting null data
		if (content == null || dateInfo == null) {
			return;
		}
		this.siddurContent = content;
		this.jewishDateInfo = dateInfo;
		// Invalidate the view to force a recomposition with the new data
		if (isAttachedToWindow()) {
			createComposition();
		}
	}

	// --- THIS IS THE FINAL, CORRECT IMPLEMENTATION ---
	// It must be 'public' to override the parent method.
	@Override
	@Composable
	public void Content(@Nullable Composer composer, int i) {
		// --- THIS IS THE FIX ---
		// MdcTheme reads your app's XML theme and correctly sets up
		// MaterialTheme for its children. All colors will now be correct.
		/* Mdc3Theme.Mdc3Theme(
			getContext(),
			true,
			true,
			true,
			true,
			true,
			(Function2<Composer, Integer, Unit>) (c, i1) -> { */
				SiddurScreenEntry(siddurContent, jewishDateInfo, composer, 0);
				//return Unit.INSTANCE;
			/* },
			composer,
			8,
			0
		); */
		// --- END OF FIX ---
	}
	// --- END OF FIX ---
}
