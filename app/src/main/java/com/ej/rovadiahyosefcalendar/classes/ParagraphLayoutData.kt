package com.ej.rovadiahyosefcalendar.classes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun AdvancedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    isJustified: Boolean,
    largeWordCount: Int, // Changed from isLargeFirstWord: Boolean
) {
    val textMeasurer = rememberTextMeasurer()
    val plainText = text.text // Work with the plain string for all logic

    Layout(
        modifier = modifier,
        content = {
            Canvas(Modifier.fillMaxWidth()) {
                val paragraphs = plainText.split("\n\n")
                var currentY = 0f

                paragraphs.forEach { paragraphText ->
                    if (paragraphText.isBlank()) return@forEach

                    val words = paragraphText.trim().split(' ')
                    val finalStyle = style.copy(textAlign = if (isJustified) TextAlign.Justify else TextAlign.Right)

                    // Check if we should use the complex layout
                    val useComplexLayout = largeWordCount > 0 && words.size > largeWordCount

                    // Find the original AnnotatedString for this paragraph to preserve styles
                    val paragraphStartIndex = text.text.indexOf(paragraphText)
                    val paragraphAnnotatedString = if (paragraphStartIndex != -1) {
                        text.subSequence(paragraphStartIndex, paragraphStartIndex + paragraphText.length)
                    } else {
                        AnnotatedString(paragraphText) // Fallback
                    }

                    if (useComplexLayout) {
                        // --- THIS IS THE NEW LOGIC ---
                        // 1. Get the large words and the rest of the text.
                        val largeWords = words.take(largeWordCount)
                        val largeWordsString = largeWords.joinToString(" ")
                        val restOfText = paragraphText.substring(paragraphText.indexOf(largeWordsString) + largeWordsString.length).trimStart()

                        // 2. Measure the block of large words.
                        val largeWordStyle = style.copy(fontSize = style.fontSize * 1.7f)
                        val largeWordsLayout = textMeasurer.measure(
                            text = largeWordsString,
                            style = largeWordStyle
                        )
                        val largeWordsWidth = largeWordsLayout.size.width
                        val largeWordsHeight = largeWordsLayout.size.height
                        // --- END OF NEW LOGIC ---

                        val spacing = 8.sp.toPx()
                        val indentedConstraints = Constraints(maxWidth = (size.width - largeWordsWidth - spacing).toInt().coerceAtLeast(0))

                        // Measure the entire rest of the text to get correct justification
                        val fullRestOfTextLayout = textMeasurer.measure(
                            text = restOfText,
                            style = finalStyle,
                            constraints = indentedConstraints,
                            overflow = TextOverflow.Visible
                        )

                        // Find the line and character index where the text beside the large words should end
                        val lineIndexBeside = fullRestOfTextLayout.getLineForVerticalPosition(largeWordsHeight.toFloat() - 1)
                        val lastCharIndexBeside = fullRestOfTextLayout.getLineEnd(
                            lineIndex = lineIndexBeside,
                            visibleEnd = true
                        )

                        // Measure the text that flows BELOW
                        val textBelowString = restOfText.substring(lastCharIndexBeside)
                        val belowTextLayout = textMeasurer.measure(
                            text = textBelowString,
                            style = finalStyle,
                            constraints = Constraints(maxWidth = size.width.toInt())
                        )

                        val besideTextHeight = fullRestOfTextLayout.getLineBottom(lineIndexBeside)

                        // --- DRAWING PASS ---
                        drawText(largeWordsLayout, topLeft = Offset(size.width - largeWordsWidth, currentY))

                        clipRect(
                            left = 0f,
                            top = currentY,
                            right = size.width - largeWordsWidth - spacing,
                            bottom = currentY + besideTextHeight
                        ) {
                            drawText(fullRestOfTextLayout, topLeft = Offset(0f, currentY))
                        }

                        drawText(belowTextLayout, topLeft = Offset(0f, currentY + besideTextHeight))

                        currentY += max(largeWordsHeight.toFloat(), besideTextHeight) + belowTextLayout.size.height

                    } else {
                        // --- SIMPLE LAYOUT (no changes here) ---
                        val simpleLayout = textMeasurer.measure(
                            text = paragraphAnnotatedString,
                            style = finalStyle,
                            constraints = Constraints(maxWidth = size.width.toInt())
                        )
                        drawText(simpleLayout, topLeft = Offset(0f, currentY))
                        currentY += simpleLayout.size.height
                    }
                }
            }
        }
    ) { measurables, constraints ->
        // --- MEASURE PASS (with the same new logic) ---
        var totalHeight = 0f
        val paragraphs = plainText.split("\n\n")

        paragraphs.forEach { paragraphText ->
            if (paragraphText.isBlank()) return@forEach
            val words = paragraphText.trim().split(' ')
            val finalStyle = style.copy(textAlign = if (isJustified) TextAlign.Justify else TextAlign.Right)

            // Check if we should use the complex layout
            val useComplexLayout = largeWordCount > 0 && words.size > largeWordCount

            if (useComplexLayout) {
                // --- THIS IS THE NEW LOGIC (mirrored) ---
                val largeWords = words.take(largeWordCount)
                val largeWordsString = largeWords.joinToString(" ")
                val largeWordStyle = style.copy(fontSize = style.fontSize * 1.7f)
                val largeWordsLayout = textMeasurer.measure(text = largeWordsString, style = largeWordStyle)
                val largeWordsHeight = largeWordsLayout.size.height
                val largeWordsWidth = largeWordsLayout.size.width
                val restOfText = paragraphText.substring(paragraphText.indexOf(largeWordsString) + largeWordsString.length).trimStart()
                // --- END OF NEW LOGIC (mirrored) ---

                val spacing = 8.sp.toPx()
                val indentedConstraints = constraints.copy(minWidth = 0, maxWidth = (constraints.maxWidth - largeWordsWidth - spacing).toInt().coerceAtLeast(0))

                val fullRestOfTextLayout = textMeasurer.measure(
                    text = restOfText,
                    style = finalStyle,
                    constraints = indentedConstraints,
                    overflow = TextOverflow.Visible
                )

                val lineIndexBeside = fullRestOfTextLayout.getLineForVerticalPosition(largeWordsHeight.toFloat() - 1)
                val lastCharIndexBeside = fullRestOfTextLayout.getLineEnd(lineIndex = lineIndexBeside, visibleEnd = true)
                val textBelowString = restOfText.substring(lastCharIndexBeside)

                val textBelowLayout = textMeasurer.measure(
                    text = textBelowString,
                    style = finalStyle,
                    constraints = constraints
                )

                val besideTextHeight = fullRestOfTextLayout.getLineBottom(lineIndexBeside)
                totalHeight += max(largeWordsHeight.toFloat(), besideTextHeight) + textBelowLayout.size.height.toFloat()
            } else {
                val simpleLayout = textMeasurer.measure(
                    text = AnnotatedString(paragraphText),
                    style = finalStyle,
                    constraints = constraints
                )
                totalHeight += simpleLayout.size.height
            }
        }

        val placeable = measurables.first().measure(
            constraints.copy(minHeight = totalHeight.toInt(), maxHeight = totalHeight.toInt())
        )
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
}
