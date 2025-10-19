package com.ej.rovadiahyosefcalendar.classes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
    largeWordCount: Int,
) {
    val textMeasurer = rememberTextMeasurer()

    Layout(
        modifier = modifier,
        content = {
            Canvas(Modifier.fillMaxWidth()) {
                // The drawing pass is mostly correct because it uses paragraphAnnotatedString.
                // We only need to ensure the height calculation is also correct.
                val paragraphs = text.text.split("\n\n")
                var currentY = 0f

                paragraphs.forEach { paragraphText ->
                    if (paragraphText.isBlank()) return@forEach

                    val words = paragraphText.trim().split(' ')
                    val finalStyle = style.copy(textAlign = if (isJustified) TextAlign.Justify else TextAlign.Right)
                    val useComplexLayout = largeWordCount > 0 && words.size > largeWordCount

                    // Find the original AnnotatedString for this paragraph to preserve styles
                    val paragraphStartIndex = text.text.indexOf(paragraphText)
                    val paragraphAnnotatedString = if (paragraphStartIndex != -1) {
                        text.subSequence(paragraphStartIndex, paragraphStartIndex + paragraphText.length)
                    } else {
                        AnnotatedString(paragraphText) // Fallback
                    }

                    if (useComplexLayout) {
                        val largeWords = words.take(largeWordCount)
                        val largeWordsString = largeWords.joinToString(" ")
                        val restOfText = paragraphText.substring(paragraphText.indexOf(largeWordsString) + largeWordsString.length).trimStart()

                        val largeWordStyle = style.copy(fontSize = style.fontSize * 1.4f)
                        val largeWordsLayout = textMeasurer.measure(text = largeWordsString, style = largeWordStyle)
                        val largeWordsWidth = largeWordsLayout.size.width
                        val largeWordsHeight = largeWordsLayout.size.height

                        val spacing = 8.sp.toPx()
                        val indentedConstraints = Constraints(maxWidth = (size.width - largeWordsWidth - spacing).toInt().coerceAtLeast(0))

                        // Find the original AnnotatedString for the rest of the text
                        val restOfTextStartIndex = paragraphAnnotatedString.text.indexOf(restOfText)
                        val restOfTextAnnotated = if (restOfTextStartIndex != -1) {
                            paragraphAnnotatedString.subSequence(restOfTextStartIndex, paragraphAnnotatedString.length)
                        } else {
                            AnnotatedString(restOfText)
                        }

                        val fullRestOfTextLayout = textMeasurer.measure(
                            text = restOfTextAnnotated, // Use rich text
                            style = finalStyle,
                            constraints = indentedConstraints,
                            overflow = TextOverflow.Visible
                        )

                        val lineIndexBeside = fullRestOfTextLayout.getLineForVerticalPosition(largeWordsHeight.toFloat() - 1)
                        val lastCharIndexBeside = fullRestOfTextLayout.getLineEnd(lineIndex = lineIndexBeside, visibleEnd = true)

                        val textBelowString = restOfTextAnnotated.subSequence(lastCharIndexBeside, restOfTextAnnotated.length)
                        val belowTextLayout = textMeasurer.measure(
                            text = textBelowString, // Use rich text
                            style = finalStyle,
                            constraints = Constraints(maxWidth = size.width.toInt())
                        )

                        val besideTextHeight = fullRestOfTextLayout.getLineBottom(lineIndexBeside)

                        drawText(largeWordsLayout, topLeft = Offset(size.width - largeWordsWidth, currentY))
                        clipRect(left = 0f, top = currentY, right = size.width - largeWordsWidth - spacing, bottom = currentY + besideTextHeight) {
                            drawText(fullRestOfTextLayout, topLeft = Offset(0f, currentY))
                        }
                        drawText(belowTextLayout, topLeft = Offset(0f, currentY + besideTextHeight))

                        currentY += max(largeWordsHeight.toFloat(), besideTextHeight) + belowTextLayout.size.height

                    } else {
                        val simpleLayout = textMeasurer.measure(
                            text = paragraphAnnotatedString, // This was already correct
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
        // --- THIS IS THE DEFINITIVE FIX: Synchronize the Measure and Draw passes ---
        var totalHeight = 0f
        val paragraphs = text.text.split("\n\n")

        paragraphs.forEach { paragraphText ->
            if (paragraphText.isBlank()) return@forEach

            // Find the original AnnotatedString for this paragraph to preserve styles
            val paragraphStartIndex = text.text.indexOf(paragraphText)
            val paragraphAnnotatedString = if (paragraphStartIndex != -1) {
                text.subSequence(paragraphStartIndex, paragraphStartIndex + paragraphText.length)
            } else {
                AnnotatedString(paragraphText) // Fallback
            }

            val words = paragraphText.trim().split(' ')
            val finalStyle = style.copy(textAlign = if (isJustified) TextAlign.Justify else TextAlign.Right)
            val useComplexLayout = largeWordCount > 0 && words.size > largeWordCount

            if (useComplexLayout) {
                val largeWords = words.take(largeWordCount)
                val largeWordsString = largeWords.joinToString(" ")
                val largeWordStyle = style.copy(fontSize = style.fontSize * 1.4f)
                val largeWordsLayout = textMeasurer.measure(text = largeWordsString, style = largeWordStyle)
                val largeWordsHeight = largeWordsLayout.size.height
                val largeWordsWidth = largeWordsLayout.size.width

                val restOfText = paragraphText.substring(paragraphText.indexOf(largeWordsString) + largeWordsString.length).trimStart()
                // Also use the rich AnnotatedString here for measurement
                val restOfTextStartIndex = paragraphAnnotatedString.text.indexOf(restOfText)
                val restOfTextAnnotated = if (restOfTextStartIndex != -1) {
                    paragraphAnnotatedString.subSequence(restOfTextStartIndex, paragraphAnnotatedString.length)
                } else {
                    AnnotatedString(restOfText)
                }

                val spacing = 8.sp.toPx()
                val indentedConstraints = constraints.copy(minWidth = 0, maxWidth = (constraints.maxWidth - largeWordsWidth - spacing).toInt().coerceAtLeast(0))

                val fullRestOfTextLayout = textMeasurer.measure(
                    text = restOfTextAnnotated, // Use rich text
                    style = finalStyle,
                    constraints = indentedConstraints,
                    overflow = TextOverflow.Visible
                )

                val lineIndexBeside = fullRestOfTextLayout.getLineForVerticalPosition(largeWordsHeight.toFloat() - 1)
                val lastCharIndexBeside = fullRestOfTextLayout.getLineEnd(lineIndex = lineIndexBeside, visibleEnd = true)
                val textBelowString = restOfTextAnnotated.subSequence(lastCharIndexBeside, restOfTextAnnotated.length)

                val textBelowLayout = textMeasurer.measure(
                    text = textBelowString, // Use rich text
                    style = finalStyle,
                    constraints = constraints
                )

                val besideTextHeight = fullRestOfTextLayout.getLineBottom(lineIndexBeside)
                totalHeight += max(largeWordsHeight.toFloat(), besideTextHeight) + textBelowLayout.size.height.toFloat()

            } else {
                // **THE FIX:** Measure the height using the rich `paragraphAnnotatedString`, not the plain `paragraphText`.
                val simpleLayout = textMeasurer.measure(
                    text = paragraphAnnotatedString, // Use the rich text here
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
