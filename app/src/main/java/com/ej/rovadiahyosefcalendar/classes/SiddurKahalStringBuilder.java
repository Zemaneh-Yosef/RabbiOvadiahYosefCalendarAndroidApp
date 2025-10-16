package com.ej.rovadiahyosefcalendar.classes;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public SiddurKahalStringBuilder addBoldFirstLetterOfStanza(boolean inline, String[] stanzas, String stanzaPrefix) {
		for (String stanza : stanzas) {
			int stanzaStart = this.length(); // capture position before appending
			super.append(stanzaPrefix).append(stanza);

			Pattern pattern = Pattern.compile("[\u05D0-\u05EA][\u0591-\u05C7]*");
			Matcher matcher = pattern.matcher(stanza);

			if (matcher.find()) {
				int start = stanzaStart + stanzaPrefix.length() + matcher.start();
				int end = stanzaStart + stanzaPrefix.length() + matcher.end();

				this.setSpan(new StyleSpan(Typeface.BOLD), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
			}

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
