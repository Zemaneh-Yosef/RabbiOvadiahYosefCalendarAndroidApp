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
                val paragraphs = text.text.split("\n\n")
                var currentY = 0f

                paragraphs.forEach { paragraphText ->
                    if (paragraphText.isBlank()) return@forEach

                    val words = paragraphText.trim().split(' ')
                    val finalStyle = style.copy(textAlign = if (isJustified) TextAlign.Justify else TextAlign.Right)
                    val useComplexLayout = largeWordCount > 0 && words.size > largeWordCount

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

                        val restOfTextStartIndex = paragraphAnnotatedString.text.indexOf(restOfText)
                        val restOfTextAnnotated = if (restOfTextStartIndex != -1) {
                            paragraphAnnotatedString.subSequence(restOfTextStartIndex, paragraphAnnotatedString.length)
                        } else {
                            AnnotatedString(restOfText)
                        }

                        val fullRestOfTextLayout = textMeasurer.measure(
                            text = restOfTextAnnotated,
                            style = finalStyle,
                            constraints = indentedConstraints,
                            overflow = TextOverflow.Visible
                        )

                        val lineIndexBeside = fullRestOfTextLayout.getLineForVerticalPosition(largeWordsHeight.toFloat() - 1)
                        val lastCharIndexBeside = fullRestOfTextLayout.getLineEnd(lineIndex = lineIndexBeside, visibleEnd = true)

                        val textBelowString = restOfTextAnnotated.subSequence(lastCharIndexBeside, restOfTextAnnotated.length).text.trimStart()
                        val textBelowAnnotatedString = AnnotatedString(textBelowString)

                        var belowTextLayoutHeight = 0f
                        val besideTextHeight = fullRestOfTextLayout.getLineBottom(lineIndexBeside)
                        // This is the spacing between the text beside the large words and the text below
                        val lineSpacing = if (style.lineHeight.isSp) style.lineHeight.toPx() / 16 else style.fontSize.toPx() / 16


                        drawText(largeWordsLayout, topLeft = Offset(size.width - largeWordsWidth, currentY))
                        clipRect(left = 0f, top = currentY, right = size.width - largeWordsWidth - spacing, bottom = currentY + besideTextHeight) {
                            drawText(fullRestOfTextLayout, topLeft = Offset(0f, currentY))
                        }

                        if (textBelowAnnotatedString.text.isNotBlank()) {
                            val belowTextLayout = textMeasurer.measure(
                                text = textBelowAnnotatedString,
                                style = finalStyle,
                                constraints = Constraints(maxWidth = size.width.toInt())
                            )
                            // Apply the line spacing when drawing the text below
                            drawText(belowTextLayout, topLeft = Offset(0f, currentY + besideTextHeight + lineSpacing))
                            belowTextLayoutHeight = belowTextLayout.size.height.toFloat()
                        }

                        // Apply the same spacing to the total height calculation
                        val extraSpacing = if (belowTextLayoutHeight > 0f) lineSpacing else 0f
                        currentY += max(largeWordsHeight.toFloat(), besideTextHeight + extraSpacing + belowTextLayoutHeight)

                    } else {
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
        var totalHeight = 0f
        val paragraphs = text.text.split("\n\n")

        paragraphs.forEach { paragraphText ->
            if (paragraphText.isBlank()) return@forEach

            val paragraphStartIndex = text.text.indexOf(paragraphText)
            val paragraphAnnotatedString = if (paragraphStartIndex != -1) {
                text.subSequence(paragraphStartIndex, paragraphStartIndex + paragraphText.length)
            } else {
                AnnotatedString(paragraphText)
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
                val restOfTextStartIndex = paragraphAnnotatedString.text.indexOf(restOfText)
                val restOfTextAnnotated = if (restOfTextStartIndex != -1) {
                    paragraphAnnotatedString.subSequence(restOfTextStartIndex, paragraphAnnotatedString.length)
                } else {
                    AnnotatedString(restOfText)
                }

                val spacing = 8.sp.toPx()
                val indentedConstraints = constraints.copy(minWidth = 0, maxWidth = (constraints.maxWidth - largeWordsWidth - spacing).toInt().coerceAtLeast(0))

                val fullRestOfTextLayout = textMeasurer.measure(
                    text = restOfTextAnnotated,
                    style = finalStyle,
                    constraints = indentedConstraints,
                    overflow = TextOverflow.Visible
                )

                val lineIndexBeside = fullRestOfTextLayout.getLineForVerticalPosition(largeWordsHeight.toFloat() - 1)
                val lastCharIndexBeside = fullRestOfTextLayout.getLineEnd(lineIndex = lineIndexBeside, visibleEnd = true)

                val textBelowString = restOfTextAnnotated.subSequence(lastCharIndexBeside, restOfTextAnnotated.length).text.trimStart()
                val textBelowAnnotatedString = AnnotatedString(textBelowString)

                var textBelowLayoutHeight = 0f
                // Same spacing logic as in the Canvas
                val lineSpacing = if (style.lineHeight.isSp) style.lineHeight.toPx() / 16 else style.fontSize.toPx() / 16

                if (textBelowAnnotatedString.text.isNotBlank()) {
                    val textBelowLayout = textMeasurer.measure(
                        text = textBelowAnnotatedString,
                        style = finalStyle,
                        constraints = constraints
                    )
                    textBelowLayoutHeight = textBelowLayout.size.height.toFloat()
                }

                val besideTextHeight = fullRestOfTextLayout.getLineBottom(lineIndexBeside)
                val extraSpacing = if (textBelowLayoutHeight > 0f) lineSpacing else 0f
                // Ensure total height calculation in the measure pass matches the drawing pass
                totalHeight += max(largeWordsHeight.toFloat(), besideTextHeight + extraSpacing + textBelowLayoutHeight)

            } else {
                val simpleLayout = textMeasurer.measure(
                    text = paragraphAnnotatedString,
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
