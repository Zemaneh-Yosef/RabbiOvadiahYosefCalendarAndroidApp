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
    isLargeFirstWord: Boolean,
    onWasClickedChange: (Boolean) -> Unit
) {
    var wasClicked by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    val plainText = text.text // Work with the plain string for all logic

    Layout(
        modifier = modifier.clickable {
            wasClicked = !wasClicked
            onWasClickedChange(wasClicked)
        },
        content = {
            Canvas(Modifier.fillMaxWidth()) {
                val paragraphs = plainText.split("\n\n")
                var currentY = 0f

                paragraphs.forEach { paragraphText ->
                    if (paragraphText.isBlank()) return@forEach

                    val words = paragraphText.trim().split(' ')
                    val finalStyle = style.copy(textAlign = if (isJustified) TextAlign.Justify else TextAlign.Right)
                    val useComplexLayout = isLargeFirstWord && words.size > 5

                    val paragraphStartIndex = text.text.indexOf(paragraphText)
                    val paragraphAnnotatedString = if (paragraphStartIndex != -1) {
                        text.subSequence(paragraphStartIndex, paragraphStartIndex + paragraphText.length)
                    } else {
                        AnnotatedString(paragraphText) // Fallback
                    }

                    if (useComplexLayout) {
                        val firstWord = words.first()
                        val restOfText = paragraphText.substring(paragraphText.indexOf(firstWord) + firstWord.length).trimStart()

                        // 1. Measure the big word
                        val largeWordStyle = style.copy(fontSize = style.fontSize * 1.7f)
                        val firstWordLayout = textMeasurer.measure(
                            text = firstWord,
                            style = largeWordStyle
                        )
                        val firstWordWidth = firstWordLayout.size.width
                        val firstWordHeight = firstWordLayout.size.height

                        // --- THIS IS THE FIX for Spacing ---
                        // Define a spacing value and subtract it from the available width.
                        val spacing = 8.sp.toPx()
                        val indentedConstraints = Constraints(maxWidth = (size.width - firstWordWidth - spacing).toInt().coerceAtLeast(0))
                        // --- END OF FIX ---

                        // 2. Measure the entire rest of the text to get correct justification
                        val fullRestOfTextLayout = textMeasurer.measure(
                            text = restOfText,
                            style = finalStyle,
                            constraints = indentedConstraints,
                            overflow = TextOverflow.Visible
                        )

                        // 3. Find the line and character index where the text beside the large word should end
                        val lineIndexBeside = fullRestOfTextLayout.getLineForVerticalPosition(firstWordHeight.toFloat() - 1)
                        val lastCharIndexBeside = fullRestOfTextLayout.getLineEnd(
                            lineIndex = lineIndexBeside,
                            visibleEnd = true
                        )

                        // 4. Measure the text that flows BELOW
                        val textBelowString = restOfText.substring(lastCharIndexBeside)
                        val belowTextLayout = textMeasurer.measure(
                            text = textBelowString,
                            style = finalStyle,
                            constraints = Constraints(maxWidth = size.width.toInt())
                        )

                        val besideTextHeight = fullRestOfTextLayout.getLineBottom(lineIndexBeside)

                        // --- DRAWING PASS ---
                        drawText(firstWordLayout, topLeft = Offset(size.width - firstWordWidth, currentY))

                        clipRect(
                            left = 0f,
                            top = currentY,
                            right = size.width - firstWordWidth - spacing,
                            bottom = currentY + besideTextHeight
                        ) {
                            drawText(fullRestOfTextLayout, topLeft = Offset(0f, currentY))
                        }

                        drawText(belowTextLayout, topLeft = Offset(0f, currentY + besideTextHeight))

                        currentY += max(firstWordHeight.toFloat(), besideTextHeight) + belowTextLayout.size.height

                    } else {
                        // --- SIMPLE LAYOUT ---
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
        // --- MEASURE PASS ---
        var totalHeight = 0f
        val paragraphs = plainText.split("\n\n")

        paragraphs.forEach { paragraphText ->
            if (paragraphText.isBlank()) return@forEach
            val words = paragraphText.trim().split(' ')
            val finalStyle = style.copy(textAlign = if (isJustified) TextAlign.Justify else TextAlign.Right)
            val useComplexLayout = isLargeFirstWord && words.size > 5

            if (useComplexLayout) {
                val firstWord = words.first()
                val largeWordStyle = style.copy(fontSize = style.fontSize * 1.7f)
                val firstWordLayout = textMeasurer.measure(text = firstWord, style = largeWordStyle)
                val firstWordHeight = firstWordLayout.size.height
                val firstWordWidth = firstWordLayout.size.width
                val restOfText = paragraphText.substring(paragraphText.indexOf(firstWord) + firstWord.length).trimStart()

                // --- APPLY SAME FIX IN MEASURE PASS ---
                val spacing = 8.sp.toPx()
                val indentedConstraints = constraints.copy(minWidth = 0, maxWidth = (constraints.maxWidth - firstWordWidth - spacing).toInt().coerceAtLeast(0))
                // --- END OF FIX ---

                val fullRestOfTextLayout = textMeasurer.measure(
                    text = restOfText,
                    style = finalStyle,
                    constraints = indentedConstraints,
                    overflow = TextOverflow.Visible
                )

                val lineIndexBeside = fullRestOfTextLayout.getLineForVerticalPosition(firstWordHeight.toFloat() - 1)
                val lastCharIndexBeside = fullRestOfTextLayout.getLineEnd(lineIndex = lineIndexBeside, visibleEnd = true)
                val textBelowString = restOfText.substring(lastCharIndexBeside)

                val textBelowLayout = textMeasurer.measure(
                    text = textBelowString,
                    style = finalStyle,
                    constraints = constraints
                )

                val besideTextHeight = fullRestOfTextLayout.getLineBottom(lineIndexBeside)
                totalHeight += max(firstWordHeight.toFloat(), besideTextHeight) + textBelowLayout.size.height.toFloat()
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