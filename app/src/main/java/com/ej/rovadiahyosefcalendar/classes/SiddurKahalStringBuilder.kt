package com.ej.rovadiahyosefcalendar.classes

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.core.text.inSpans

// This is the core builder function. It's much cleaner than a class.
fun siddurKahalBuilder(init: SiddurKahalStringBuilder.() -> Unit): CharSequence {
    return SiddurKahalStringBuilder().apply(init).build()
}

// The class is now a "context" for our DSL, containing the helper methods.
class SiddurKahalStringBuilder(
    val builder: SpannableStringBuilder = SpannableStringBuilder()
) {

    fun append(text: CharSequence): SiddurKahalStringBuilder {
        builder.append(text)
        return this
    }

    fun appendSmallText(smallText: String): SiddurKahalStringBuilder {
        builder.inSpans(RelativeSizeSpan(0.7f)) {
            append(smallText)
        }
        return this
    }

    fun appendColorText(colorText: String, color: Int): SiddurKahalStringBuilder {
        builder.inSpans(ForegroundColorSpan(color)) {
            append(colorText)
        }
        return this
    }

    fun appendAmen(): SiddurKahalStringBuilder {
        return appendSmallText(" [אמן] ")
    }

    fun addRepeatedKahalChorusSmallText(
        inBetween: String = "\n",
        stanzas: Array<String>,
        chorus: String,
        stanzaPrefix: String = ""
    ): SiddurKahalStringBuilder {
        stanzas.forEachIndexed { index, stanza ->
            append(stanzaPrefix)
            append(stanza)
            appendSmallText(chorus)

            if (inBetween.isNotEmpty() && index < stanzas.size - 1) {
                append(inBetween)
            }
        }
        return this
    }

    fun addRepeatedKahalChorusSmallText(
        inline: Boolean,
        stanzas: Array<String>,
        chorus: String,
        stanzaPrefix: String = ""
    ): SiddurKahalStringBuilder {
        return addRepeatedKahalChorusSmallText(if (inline) "" else "\n", stanzas, chorus, stanzaPrefix)
    }

    fun addRepeatedKahalChorusSmallText(
        stanzas: Array<String>,
        chorus: String,
        stanzaPrefix: String = ""
    ): SiddurKahalStringBuilder {
        return addRepeatedKahalChorusSmallText("\n", stanzas, chorus, stanzaPrefix)
    }

    fun addRepeatedKahalChorusSmallText(
        stanzas: Array<String>,
        chorus: String
    ): SiddurKahalStringBuilder {
        return addRepeatedKahalChorusSmallText("\n", stanzas, chorus, "")
    }

    fun addBoldFirstLetterOfStanza(
        inBetween: String = "\n",
        stanzas: Array<String>,
        stanzaPrefix: String = ""
    ): SiddurKahalStringBuilder {
        val hebrewLetterPattern = "[\u05D0-\u05EA][\u0591-\u05C7]*".toRegex()
        stanzas.forEachIndexed { index, stanza ->
            val fullStanza = stanzaPrefix + stanza
            val match = hebrewLetterPattern.find(fullStanza)

            if (match != null) {
                // Append the part before the bold letter
                append(fullStanza.substring(0, match.range.first))
                // Append the bold letter itself
                builder.inSpans(StyleSpan(Typeface.BOLD)) {
                    append(match.value)
                }
                // Append the rest of the stanza
                append(fullStanza.substring(match.range.last + 1))
            } else {
                append(fullStanza) // Append normally if no letter found
            }

            if (inBetween.isNotEmpty() && index < stanzas.size - 1) {
                append(inBetween)
            }
        }
        return this;
    }

    fun addBoldFirstLetterOfStanza(
        inline: Boolean,
        stanzas: Array<String>,
        stanzaPrefix: String = ""
    ): SiddurKahalStringBuilder {
        return addBoldFirstLetterOfStanza(if (inline) "" else "\n", stanzas, stanzaPrefix)
    }

    fun addBoldFirstLetterOfStanzaForKahal(
        inBetween: String = "\n",
        stanzas: Array<String>,
        stanzaPrefix: String = ""
    ): SiddurKahalStringBuilder {
        val hebrewLetterPattern = "[\u05D0-\u05EA][\u0591-\u05C7]*".toRegex()
        stanzas.forEachIndexed { index, stanza ->
            val fullStanza = stanzaPrefix + stanza
            val match = hebrewLetterPattern.find(fullStanza)

            if (match != null) {
                // Append the part before the bold letter
                append(fullStanza.substring(0, match.range.first))
                // Append the bold letter itself
                builder.inSpans(StyleSpan(Typeface.BOLD)) {
                    append(match.value)
                }
                // Append the rest of the stanza
                append(fullStanza.substring(match.range.last + 1))
            } else {
                append(fullStanza) // Append normally if no letter found
            }

            if (inBetween.isNotEmpty() && index < stanzas.size - 1) {
                appendSmallText(inBetween)
            }
        }
        return this;
    }

    // You can add your other helper methods here following the same pattern
    fun length(): Int {
        return builder.length
    }

    fun delete(start: Int, end: Int): SiddurKahalStringBuilder? {
        builder.delete(start, end)
        return this
    }

    // This method returns the final result.
    fun build(): CharSequence {
        return builder
    }

    fun clone(): SiddurKahalStringBuilder {
        // 1. Create a new SpannableStringBuilder using the constructor that
        //    is documented to copy both TEXT and SPANS from the source.
        val clonedSpannable = SpannableStringBuilder(this.builder)

        // 2. Create a new instance of our wrapper class with this perfect clone.
        return SiddurKahalStringBuilder(clonedSpannable)
    }

    fun clear() {
        builder.clear()
        builder.clearSpans()
    }
}
