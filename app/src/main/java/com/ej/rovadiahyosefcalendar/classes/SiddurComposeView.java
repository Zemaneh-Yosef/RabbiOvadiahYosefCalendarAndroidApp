package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.Composer;
import androidx.compose.ui.platform.AbstractComposeView;

import java.util.ArrayList;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static com.ej.rovadiahyosefcalendar.classes.SiddurScreenKt.SiddurScreenEntry;

public class SiddurComposeView extends AbstractComposeView {

	private ArrayList<HighlightString> siddurContent = new ArrayList<>();
	private JewishDateInfo jewishDateInfo = new JewishDateInfo(false);

	// Create a field to hold the scrolling function given to us by Compose.
	private Function1<Integer, Unit> scrollToPositionLambda = null;

	public SiddurComposeView(@NonNull Context context) {
		super(context);
	}

	public SiddurComposeView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public SiddurComposeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	// This method is called from the Activity to trigger the scroll.
	public void scrollToPosition(int position) {
		// If we have received the scrolling lambda from Compose, invoke it.
		if (scrollToPositionLambda != null) {
			scrollToPositionLambda.invoke(position);
		}
	}

	// The Activity calls this method to provide the data.
	public void setData(ArrayList<HighlightString> content, JewishDateInfo dateInfo) {
		if (content == null || dateInfo == null) {
			return;
		}
		this.siddurContent = content;
		this.jewishDateInfo = dateInfo;
		if (isAttachedToWindow()) {
			createComposition();
		}
	}

	@Override
	@Composable
	public void Content(@Nullable Composer composer, int i) {
		SiddurScreenEntry(
			siddurContent,
			jewishDateInfo,
			// Update the cast to match the expected type with wildcards
			(Function1<? super Function1<? super Integer, Unit>, Unit>) scrollLambda -> {
				this.scrollToPositionLambda = (Function1<Integer, Unit>) scrollLambda;
				return Unit.INSTANCE;
			},
			composer,
			0
		);
	}

}
