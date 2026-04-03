package com.ej.rovadiahyosefcalendar.classes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlin.math.max

// Holds all pre-measured layout data for a single paragraph.
// Produced by the Layout pass, consumed by the Canvas draw pass.
private sealed class ParagraphLayout {

	// A paragraph that fits in a single block with no large words.
	data class Simple(
		val layout: TextLayoutResult,
	) : ParagraphLayout()

	// A paragraph with large words at the start, text beside them,
	// and optionally text below once the large words end.
	data class Complex(
		val largeWordsLayout: TextLayoutResult,
		val besideLayout: TextLayoutResult,
		val belowLayout: TextLayoutResult?,
		val largeWordsWidth: Float,
		val besideHeight: Float,
		val lineSpacing: Float,
	) : ParagraphLayout()
}

// Total pixel height of a paragraph layout.
private fun ParagraphLayout.totalHeight(): Float = when (this) {
	is ParagraphLayout.Simple -> layout.size.height.toFloat()
	is ParagraphLayout.Complex -> {
		val belowHeight = belowLayout?.size?.height?.toFloat() ?: 0f
		val extraSpacing = if (belowHeight > 0f) lineSpacing else 0f
		max(
			largeWordsLayout.size.height.toFloat(),
			besideHeight + extraSpacing + belowHeight
		)
	}
}

@Composable
fun AdvancedText(
	text: AnnotatedString,
	modifier: Modifier = Modifier,
	style: TextStyle = TextStyle.Default,
	isJustified: Boolean,
	largeWordCount: Int,
) {
	val textMeasurer = rememberTextMeasurer()

	// Shared cache: the Layout pass fills this, the Canvas pass reads it.
	// Keyed on everything that affects measurement so it resets when input changes.
	val measuredLayouts = remember(text, style, isJustified, largeWordCount) {
		mutableListOf<ParagraphLayout>()
	}

	// Pre-split paragraphs and track positions once, shared by both passes.
	// Using a running index avoids indexOf() finding the wrong occurrence
	// of repeated prayer text.
	data class ParagraphEntry(
		val annotatedString: AnnotatedString,
		val words: List<String>,
	)

	val paragraphEntries = remember(text) {
		var currentIndex = 0
		text.text.split("\n\n").mapNotNull { paragraphText ->
			val start = currentIndex
			val end = (start + paragraphText.length).coerceAtMost(text.length)
			currentIndex = end + 2 // skip the "\n\n"

			if (paragraphText.isBlank()) return@mapNotNull null

			ParagraphEntry(
				annotatedString = text.subSequence(start, end),
				words = paragraphText.trim().split(' '),
			)
		}
	}

	Layout(
		modifier = modifier,
		content = {
			Canvas(Modifier.fillMaxWidth()) {
				// Draw pass: consume measuredLayouts filled by the Layout pass.
				// No measuring happens here — just drawing.
				var currentY = 0f

				measuredLayouts.forEach { paragraphLayout ->
					when (paragraphLayout) {
						is ParagraphLayout.Simple -> {
							drawText(paragraphLayout.layout, topLeft = Offset(0f, currentY))
							currentY += paragraphLayout.layout.size.height
						}

						is ParagraphLayout.Complex -> {
							val largeWordsWidth = paragraphLayout.largeWordsWidth
							val largeWordsHeight = paragraphLayout.largeWordsLayout.size.height.toFloat()
							val besideHeight = paragraphLayout.besideHeight
							val lineSpacing = paragraphLayout.lineSpacing
							val besideWidth = size.width - largeWordsWidth - 8.sp.toPx()
							val belowHeight = paragraphLayout.belowLayout?.size?.height?.toFloat() ?: 0f
							val extraSpacing = if (belowHeight > 0f) lineSpacing else 0f

							drawText(
								paragraphLayout.largeWordsLayout,
								topLeft = Offset(size.width - largeWordsWidth, currentY)
							)
							clipRect(
								left = 0f,
								top = currentY,
								right = besideWidth,
								bottom = currentY + besideHeight
							) {
								drawText(
									paragraphLayout.besideLayout,
									topLeft = Offset(0f, currentY)
								)
							}
							paragraphLayout.belowLayout?.let { belowLayout ->
								drawText(
									belowLayout,
									topLeft = Offset(
										size.width - belowLayout.size.width,
										currentY + besideHeight + lineSpacing
									)
								)
							}

							currentY += max(
								largeWordsHeight,
								besideHeight + extraSpacing + belowHeight
							)
						}
					}
				}
			}
		}
	) { measurables, constraints ->
		// Measure pass: do all measuring here with real constraints,
		// store results in measuredLayouts for the Canvas draw pass.
		measuredLayouts.clear()

		val finalStyle = style.copy(
			textAlign = if (isJustified) TextAlign.Justify else TextAlign.Right
		)
		val lineSpacing = if (style.lineHeight.isSp) {
			style.lineHeight.toPx() / 16f
		} else {
			style.fontSize.toPx() / 16f
		}

		var totalHeight = 0f

		paragraphEntries.forEach { entry ->
			val useComplexLayout = largeWordCount > 0 && entry.words.size > largeWordCount

			if (useComplexLayout) {
				val largeWords = entry.words.take(largeWordCount)
				val largeWordsString = largeWords.joinToString(" ")
				val largeWordStyle = style.copy(fontSize = style.fontSize * 1.4f)

				val largeWordsLayout = textMeasurer.measure(
					text = largeWordsString,
					style = largeWordStyle
				)
				val largeWordsWidth = largeWordsLayout.size.width.toFloat()
				val largeWordsHeight = largeWordsLayout.size.height.toFloat()
				val spacing = 8.sp.toPx()

				// Derive restOfText as AnnotatedString so spans are preserved.
				val largeWordsEndIndex = entry.annotatedString.text
					.indexOf(largeWordsString)
					.plus(largeWordsString.length)
					.coerceAtMost(entry.annotatedString.length)

				val restOfTextAnnotated = entry.annotatedString
					.subSequence(largeWordsEndIndex, entry.annotatedString.length)
					.let { raw ->
						val trimOffset = raw.length - raw.text.trimStart().length
						if (trimOffset > 0) raw.subSequence(trimOffset, raw.length) else raw
					}

				val besideConstraints = constraints.copy(
					minWidth = 0,
					maxWidth = (constraints.maxWidth - largeWordsWidth - spacing)
						.toInt()
						.coerceAtLeast(0)
				)

				val besideLayout = textMeasurer.measure(
					text = restOfTextAnnotated,
					style = finalStyle,
					constraints = besideConstraints,
					overflow = TextOverflow.Visible
				)

				val lineIndexBeside = besideLayout
					.getLineForVerticalPosition(largeWordsHeight - 1f)
				val lastCharIndexBeside = besideLayout
					.getLineEnd(lineIndex = lineIndexBeside, visibleEnd = true)
				val besideHeight = besideLayout.getLineBottom(lineIndexBeside)

				// Extract below text as AnnotatedString, preserving spans.
				val belowLayout = if (lastCharIndexBeside < restOfTextAnnotated.length) {
					val belowAnnotated = restOfTextAnnotated
						.subSequence(lastCharIndexBeside, restOfTextAnnotated.length)
						.let { raw ->
							val trimOffset = raw.length - raw.text.trimStart().length
							if (trimOffset > 0) raw.subSequence(trimOffset, raw.length) else raw
						}
					if (belowAnnotated.text.isNotBlank()) {
						textMeasurer.measure(
							text = belowAnnotated,
							style = finalStyle,
							constraints = constraints
						)
					} else null
				} else null

				val belowHeight = belowLayout?.size?.height?.toFloat() ?: 0f
				val extraSpacing = if (belowHeight > 0f) lineSpacing else 0f

				measuredLayouts.add(
					ParagraphLayout.Complex(
						largeWordsLayout = largeWordsLayout,
						besideLayout = besideLayout,
						belowLayout = belowLayout,
						largeWordsWidth = largeWordsWidth,
						besideHeight = besideHeight,
						lineSpacing = lineSpacing,
					)
				)

				totalHeight += max(
					largeWordsHeight,
					besideHeight + extraSpacing + belowHeight
				)
			} else {
				val layout = textMeasurer.measure(
					text = entry.annotatedString,
					style = finalStyle,
					constraints = constraints
				)
				measuredLayouts.add(ParagraphLayout.Simple(layout = layout))
				totalHeight += layout.size.height
			}
		}

		val placeable = measurables.first().measure(
			constraints.copy(
				minHeight = totalHeight.toInt(),
				maxHeight = totalHeight.toInt()
			)
		)
		layout(placeable.width, placeable.height) {
			placeable.placeRelative(0, 0)
		}
	}
}