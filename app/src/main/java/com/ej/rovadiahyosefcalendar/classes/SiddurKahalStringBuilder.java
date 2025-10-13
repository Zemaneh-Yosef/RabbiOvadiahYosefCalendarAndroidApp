package com.ej.rovadiahyosefcalendar.classes;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.NonNull;

public class SiddurKahalStringBuilder extends SpannableStringBuilder {
	public SiddurKahalStringBuilder() {
		super();
	}

	public SiddurKahalStringBuilder(SiddurKahalStringBuilder kaddishText) {
		super(kaddishText);
	}

	public SiddurKahalStringBuilder addRepeatedKahalChorusSmallText(boolean inline, String[] stanzas, String chorus, String stanzaPrefix) {
		for (String stanza: stanzas) {
			super.append(stanzaPrefix).append(stanza);
			this.appendSmallText(chorus);

			if (!inline)
				super.append("\n");
		}

		if (!inline)
			super.delete(super.length() - 1, super.length());
		return this;
	}

	public SiddurKahalStringBuilder addRepeatedKahalChorusSmallText(String[] stanzas, String chorus, String stanzaPrefix) {
		return this.addRepeatedKahalChorusSmallText(false, stanzas, chorus, stanzaPrefix);
	}

	public SiddurKahalStringBuilder addRepeatedKahalChorusSmallText(String[] stanzas, String chorus) {
		return this.addRepeatedKahalChorusSmallText(stanzas, chorus, "");
	}

	public SiddurKahalStringBuilder appendSmallText(String smallText) {
		int preSmall = super.length();
		super.append(smallText);
		super.setSpan(new RelativeSizeSpan(0.7f), preSmall, super.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		return this;
	}

	public SiddurKahalStringBuilder appendAmen() {
		return appendSmallText(" [אמן] ");
	}

	@NonNull
	@Override
	public SiddurKahalStringBuilder append(char text) {
		super.append(text);
		return this;
	}

	@NonNull
	public SiddurKahalStringBuilder append(String text) {
		super.append(text);
		return this;
	}
}
