package com.ej.rovadiahyosefcalendar.classes;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;

public class SiddurKahalStringBuilder extends SpannableStringBuilder {
	public SiddurKahalStringBuilder addRepeatedKahalChorusSmallText(String[] stanzas, String chorus, String stanzaPrefix) {
		for (String stanza: stanzas) {
			super.append(stanzaPrefix + stanza);

			int preChorusLength = super.length();
			super.append(chorus);
			super.setSpan(new RelativeSizeSpan(0.7f), preChorusLength, super.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			super.append("\n");
		}

		super.delete(super.length() - 1, super.length());
		return this;
	}

	public SiddurKahalStringBuilder addRepeatedKahalChorusSmallText(String[] stanzas, String chorus) {
		return this.addRepeatedKahalChorusSmallText(stanzas, chorus, "");
	}
}
