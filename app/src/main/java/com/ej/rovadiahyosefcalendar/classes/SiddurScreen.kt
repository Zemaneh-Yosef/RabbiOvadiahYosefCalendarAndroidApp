package com.ej.rovadiahyosefcalendar.classes

import android.content.Context
import androidx.core.content.ContextCompat
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.ej.rovadiahyosefcalendar.R as AppR
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import com.aghajari.compose.text.asAnnotatedString


@Composable
fun SiddurScreenEntry(
    siddurContent: List<HighlightString>,
    jewishDateInfo: JewishDateInfo
) {
    SiddurScreen(siddurContent = siddurContent, jewishDateInfo = jewishDateInfo)
}

// This is a powerful helper function that reads a color from your app's XML theme.
@Composable
fun getThemeColor(colorAttr: Int): Color {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    context.theme.resolveAttribute(colorAttr, typedValue, true)

    // --- THIS IS THE FIX ---
    // Instead of using typedValue.data directly, we use ContextCompat.
    // This correctly resolves the color from the resource ID, handling selectors.
    val colorRes = if (typedValue.resourceId != 0) {
        typedValue.resourceId
    } else {
        typedValue.data // Fallback for direct color values
    }
    // We need to convert the Android Color (Int) to a Compose Color
    return Color(ContextCompat.getColor(context, colorRes))
    // --- END OF FIX ---
}

@Composable
private fun SiddurScreen(
    siddurContent: List<HighlightString>,
    jewishDateInfo: JewishDateInfo
) {
    val context = LocalContext.current
    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // State for our UI controls
    var textSize by remember { mutableFloatStateOf(sharedPreferences.getInt("TextSize", 18).toFloat()) }
    var isJustified by remember { mutableStateOf(sharedPreferences.getBoolean("isJustified", false)) }

    val fontPreference = remember { sharedPreferences.getString("font", "Guttman Keren") ?: "Guttman Keren" }

    // Use our helper to get the exact background color from the app's theme.
    val defaultBackgroundColor = getThemeColor(android.R.attr.colorBackground)

    Scaffold(
        backgroundColor = defaultBackgroundColor, // This will now be correct for light and dark mode
        bottomBar = {
            SiddurBottomBar(
                currentTextSize = textSize,
                isJustified = isJustified,
                onTextSizeChange = { newSize ->
                    textSize = newSize
                    sharedPreferences.edit { putInt("TextSize", newSize.toInt()) }
                },
                onJustifyClick = {
                    isJustified = !isJustified
                    sharedPreferences.edit { putBoolean("isJustified", !isJustified) }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding)
        ) {
            items(siddurContent) { currentText ->
                SiddurRow(
                    currentText = currentText,
                    textSize = textSize.toInt(),
                    isJustified = isJustified,
                    fontPreference = fontPreference,
                    jewishDateInfo = jewishDateInfo,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun SiddurBottomBar(
    currentTextSize: Float,
    isJustified: Boolean,
    onTextSizeChange: (Float) -> Unit,
    onJustifyClick: () -> Unit
) {
    BottomAppBar( // Use a standard BottomAppBar for correct elevation/coloring
        backgroundColor = getThemeColor(android.R.attr.windowBackground),
        contentColor = MaterialTheme.colors.onSurface
    ) {
        // Justification Toggle Button
        IconButton(onClick = onJustifyClick) {
            Icon(
                painter = painterResource(id = AppR.drawable.baseline_format_align_justify_24),
                contentDescription = stringResource(AppR.string.justify_text),
                tint = if (isJustified) MaterialTheme.colors.primary else LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            )
        }

        // Text Size Slider
        Slider(
            value = currentTextSize,
            onValueChange = onTextSizeChange,
            valueRange = 18f..45f,
            steps = 23,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF673AB7),
                activeTrackColor = Color(0xFF673AB7).copy(alpha = 0.7f),
                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f),
                activeTickColor = Color(0xFF9C27B0).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SiddurRow(
    currentText: HighlightString,
    textSize: Int,
    isJustified: Boolean,
    fontPreference: String,
    jewishDateInfo: JewishDateInfo,
    context: Context
) {
    val isNightMode = isSystemInDarkTheme()
    val currentTextType = currentText.type
    val text = if (currentText.isSpannableString) currentText.spannableString.asAnnotatedString().annotatedString else AnnotatedString(currentText.toString())

    if (text.text == "[break here]") {
        val color = if (isNightMode) Color.White else Color.Black
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(1.dp)
                .background(color)
        )
        return
    }

    val rowBackgroundColor = when (currentTextType) {
        HighlightString.StringType.HIGHLIGHT -> if (isNightMode) colorResource(AppR.color.goldenrod) else colorResource(AppR.color.mainly_BLUE)
        HighlightString.StringType.INFO -> Color.DarkGray
        else -> Color.Transparent
    }

    Column(modifier = Modifier.background(rowBackgroundColor)) {
        val isParagraph = currentTextType == HighlightString.StringType.PARAGRAPH
        val isCategory = currentTextType == HighlightString.StringType.CATEGORY
        val isInstruction = currentTextType == HighlightString.StringType.INSTRUCTION
        val isInfo = currentTextType == HighlightString.StringType.INFO

        val textColor = when {
            currentTextType == HighlightString.StringType.HIGHLIGHT -> Color.Black
            else -> getThemeColor(android.R.attr.textColorPrimary) // Use your helper to get the theme's primary text color
        }

        val fontFamily = when {
            isCategory -> FontFamily(Font(AppR.font.mantb_2))
            isInstruction -> FontFamily(Font(AppR.font.spectral_bold, FontWeight.Bold))
            fontPreference == "Taamey Frank" -> FontFamily(Font(AppR.font.taamey_d))
            else -> FontFamily(Font(AppR.font.guttman_keren))
        }

        val finalSize = when {
            isCategory -> textSize + 8
            isInstruction -> textSize - 10
            else -> textSize
        }

        val isRTL = if (text.isEmpty()) {
                false
            } else {
                val firstChar = text.text[0]
                val directionality = Character.getDirectionality(firstChar)
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
            }
        val textDirection = if (isRTL) { androidx.compose.ui.text.style.TextDirection.Rtl } else {androidx.compose.ui.text.style.TextDirection.Ltr}

        val textAlign = when {
            isCategory || isInstruction -> TextAlign.Center
            isJustified -> TextAlign.Justify
            else -> TextAlign.Right // Default to Right-aligned
        }

        val textStyle = TextStyle(
            color = textColor,
            fontSize = finalSize.sp,
            fontFamily = fontFamily,
            textAlign = textAlign,
            lineHeight = (finalSize * 1.2).sp,
            textDirection = textDirection
        )

        // The simple, correct logic you wanted from the beginning:
        if (isParagraph || isInfo) {
            var isInfoExpanded by remember { mutableStateOf(false) }
            val textToDisplay = when {
                isInfo -> if (isInfoExpanded) AnnotatedString("▼ ${currentText.summary}${text}") else AnnotatedString("▲ ${currentText.summary}")
                else -> text // Also wrap plain string for consistency
            }

            // The 'isLargeFirstWord' flag is now determined SOLELY by the StringType.
            // Our AdvancedText composable has an internal check to not apply this to short paragraphs.
            AdvancedText(
                text = textToDisplay,
                style = textStyle,
                isJustified = isJustified,
                isLargeFirstWord = isParagraph, // ALWAYS true for paragraphs
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onWasClickedChange = { // Restore the click logic
                    if (isInfo) {
                        isInfoExpanded = !isInfoExpanded
                    } else if (text.text == "Open Sefaria Siddur/פתח את סידור ספריה") {
                        val browserIntent = Intent(Intent.ACTION_VIEW,
                            "https://www.sefaria.org/Siddur_Edot_HaMizrach?tab=contents".toUri())
                        context.startActivity(browserIntent)
                    } else if (text.text == "Mussaf is said here, press here to go to Mussaf" || text.text == "מוסף אומרים כאן, לחץ כאן כדי להמשיך למוסף") {
                        val intent = Intent(context, SiddurViewActivity::class.java).apply {
                            putExtra("prayer", "מוסף")
                            putExtra("JewishDay", jewishDateInfo.jewishCalendar.jewishDayOfMonth)
                            putExtra("JewishMonth", jewishDateInfo.jewishCalendar.jewishMonth)
                            putExtra("JewishYear", jewishDateInfo.jewishCalendar.jewishYear)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        } else { // For CATEGORY, INSTRUCTION, etc.
            Text(
                text = text,
                style = textStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        if (text.text.endsWith("כׇּל־אַפְסֵי־אָֽרֶץ׃")) {
            Image(
                painter = painterResource(id = AppR.drawable.menorah),
                contentDescription = "Menorah",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
            )
        }
    }
}
