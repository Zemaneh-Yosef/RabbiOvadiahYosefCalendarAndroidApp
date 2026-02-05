package com.ej.rovadiahyosefcalendar.classes

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.SpannableString
import android.util.TypedValue
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.aghajari.compose.text.asAnnotatedString
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity
import kotlinx.coroutines.launch
import java.util.LinkedList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import com.ej.rovadiahyosefcalendar.R as AppR

// 1. Define a type for our scroll event lambda
typealias ScrollToPosition = (Int) -> Unit

// 2. The entry point now accepts a function that provides the scroll action
@Composable
fun SiddurScreenEntry(
    siddurContent: List<HighlightString>,
    jewishDateInfo: JewishDateInfo,
    onScrollRequested: (ScrollToPosition) -> Unit
) {
    SiddurScreen(
        siddurContent = siddurContent,
        jewishDateInfo = jewishDateInfo,
        onScrollRequested = onScrollRequested
    )
}

@Composable
fun getThemeColor(colorAttr: Int): Color {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    context.theme.resolveAttribute(colorAttr, typedValue, true)
    val colorInt = if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
        typedValue.data
    } else {
        ContextCompat.getColor(context, typedValue.resourceId)
    }
    return Color(colorInt)
}

@Composable
private fun SiddurScreen(
    siddurContent: List<HighlightString>,
    jewishDateInfo: JewishDateInfo,
    onScrollRequested: (ScrollToPosition) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var textSize by remember { mutableFloatStateOf(sharedPreferences.getInt("TextSize", 24).toFloat()) }
    var isJustified by remember { mutableStateOf(sharedPreferences.getBoolean("isJustified", false)) }
    val fontPreference = remember { sharedPreferences.getString("font", "Guttman Keren") ?: "Guttman Keren" }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val scrollToPosition: ScrollToPosition = { position ->
        coroutineScope.launch {
            listState.animateScrollToItem(position)
        }
    }

    LaunchedEffect(scrollToPosition) {
        onScrollRequested(scrollToPosition)
    }

    val defaultBackgroundColor = getThemeColor(android.R.attr.colorBackground)

    Scaffold(
        containerColor = defaultBackgroundColor, // M3 uses containerColor
        bottomBar = {
            SiddurBottomBar(
                currentTextSize = textSize,
                isJustified = isJustified,
                onTextSizeChange = { newSize ->
                    sharedPreferences.edit { putInt("TextSize", newSize.toInt()) }
                },
                onJustifyClick = {
                    isJustified = !isJustified
                    sharedPreferences.edit { putBoolean("isJustified", isJustified) }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = innerPadding
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
    BottomAppBar(
        containerColor = getThemeColor(android.R.attr.windowBackground),
        contentColor = getThemeColor(android.R.attr.textColorPrimary) // M3 uses contentColor
    ) {
        Slider(
            value = currentTextSize,
            onValueChange = onTextSizeChange,
            valueRange = 18f..45f,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onJustifyClick) {
            Icon(
                painter = painterResource(id = AppR.drawable.baseline_format_align_justify_24),
                contentDescription = stringResource(AppR.string.justify_text),
                tint = if (isJustified) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
    }
}

@Composable
private fun Compass(instructionalText: String) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    val magnetometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    var degree by remember { mutableFloatStateOf(0f) }

    // --- Track smoothed rotation between readings ---
    var previousDegree by remember { mutableFloatStateOf(0f) }
    var smoothDegree by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val accelerometerReading = FloatArray(3)
        val magnetometerReading = FloatArray(3)
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val window = LinkedList<Double>()

        fun filtrate(value: Double): Float {
            window.add(value)
            if (window.size > 50) {
                window.remove()
            }
            var sumx = 0.0
            var sumy = 0.0
            for (d in window) {
                sumx += cos(d / 360 * (2 * Math.PI))
                sumy += sin(d / 360 * (2 * Math.PI))
            }
            val avgx = sumx / window.size
            val avgy = sumy / window.size
            val temp = atan2(avgy, avgx) / (2 * Math.PI) * 360
            return if (temp < 0) (temp + 360).toFloat() % 360 else temp.toFloat()
        }

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                }

                if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble())
                    degree = filtrate(azimuthDegrees)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // --- Handle wraparound smoothing ---
    val displayDegree = remember(degree) {
        var delta = degree - previousDegree
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        smoothDegree += delta
        smoothDegree
    }

    val animatedDegree by animateFloatAsState(
        targetValue = -displayDegree,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow // try StiffnessMedium for snappier movement
        ),
        label = "CompassRotation"
    )

    if (accelerometer != null || magnetometer != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = instructionalText,
                color = Color.Yellow,
                textAlign = TextAlign.Center
            )
            Image(
                painter = painterResource(id = AppR.drawable.compass),
                contentDescription = "Compass",
                modifier = Modifier
                    .fillMaxWidth(0.8f) // take 80% of screen width
                    .aspectRatio(1f)
                    .rotate(animatedDegree)
            )
        }
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
    if (currentText.imageAttachment == HighlightString.ImageAttachment.COMPASS) {
        Compass(instructionalText = currentText.toString())
        return
    }

    val isNightMode = isSystemInDarkTheme()
    val currentTextType = currentText.type
    val text = SpannableString(currentText.content).asAnnotatedString().annotatedString

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

    val rowBackgroundColor = when {
        (currentTextType == HighlightString.StringType.INFO || currentTextType == HighlightString.StringType.INVERSE_INFO) -> (if (isNightMode) Color.DarkGray else Color.LightGray)
        currentText.isHighlighted -> colorResource(if (isNightMode) AppR.color.goldenrod else AppR.color.mainly_BLUE)
        else -> Color.Transparent
    }

    Column(modifier = Modifier.background(rowBackgroundColor)) {
        val isParagraph = currentText.bigWordsStart > 0
        val isCategory = currentTextType == HighlightString.StringType.CATEGORY
        val isInstruction = currentTextType == HighlightString.StringType.INSTRUCTION
        val isInfo = currentTextType == HighlightString.StringType.INFO
        val isInverseInfo = currentTextType == HighlightString.StringType.INVERSE_INFO

        val textColor = when {
            currentText.isHighlighted -> Color.Black
            else -> getThemeColor(android.R.attr.textColorPrimary)
        }

        val fontFamily = when {
            isCategory -> FontFamily(Font(AppR.font.mantb_2))
            isInstruction -> FontFamily(Font(AppR.font.spectral_bold, FontWeight.Bold))
            fontPreference == "Taamey Frank" -> FontFamily(Font(AppR.font.taamey_d))
            fontPreference == "Guttman Keren" -> FontFamily(Font(AppR.font.guttman_keren))
            else -> FontFamily.Default
        }

        val finalSize = when {
            isCategory -> textSize + 8
            isInstruction -> textSize - 6
            else -> textSize
        }

        val textDirection = remember(text.text) {
            var isRtl = false
            for (char in text.text) {
                when (Character.getDirectionality(char)) {
                    Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                    Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> {
                        isRtl = true
                        break
                    }
                    Character.DIRECTIONALITY_LEFT_TO_RIGHT -> {
                        isRtl = false
                        break
                    }
                    else -> continue
                }
            }
            if (isRtl) {
                androidx.compose.ui.text.style.TextDirection.Rtl
            } else {
                androidx.compose.ui.text.style.TextDirection.Ltr
            }
        }

        val textAlign = when {
            isCategory || isInstruction -> TextAlign.Center
            isJustified -> TextAlign.Justify
            else -> TextAlign.Right
        }

        val textStyle = TextStyle(
            color = textColor,
            fontSize = finalSize.sp,
            fontFamily = fontFamily,
            textAlign = textAlign,
            lineHeight = (finalSize * 1.2).sp,
            textDirection = textDirection
        )

        if (isParagraph) {
            Box {
                AdvancedText(
                    text = text,
                    style = textStyle,
                    isJustified = isJustified,
                    largeWordCount = currentText.bigWordsStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                SelectionContainer {
                    Text(
                        text = text,
                        style = textStyle,
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        } else {
            var isInfoExpanded by remember { mutableStateOf(false) }
            val textToDisplay = when {
                isInfo -> if (isInfoExpanded) AnnotatedString("▼ ${currentText.summary}${text}") else AnnotatedString("▲ ${currentText.summary}")
                isInverseInfo -> if (isInfoExpanded) AnnotatedString("▲ ${currentText.summary}") else AnnotatedString("▼ ${currentText.summary}${text}")
                else -> text
            }

            SelectionContainer {
                Text(
                    text = textToDisplay,
                    style = textStyle,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (isInfo || isInverseInfo) {
                                    isInfoExpanded = !isInfoExpanded
                                } else if (text.text == "Open Sefaria Siddur" || text.text == "פתח את סידור ספריה") {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://www.sefaria.org/Siddur_Edot_HaMizrach?tab=contents".toUri()
                                    )
                                    context.startActivity(browserIntent)
                                } else if (text.text == "Mussaf is said here, press here to go to Mussaf" || text.text == "מוסף אומרים כאן, לחץ כאן כדי להמשיך למוסף") {
                                    val intent = Intent(context, SiddurViewActivity::class.java).apply {
                                        putExtra("prayer", "מוסף")
                                        putExtra(
                                            "JewishDay",
                                            jewishDateInfo.jewishCalendar.jewishDayOfMonth
                                        )
                                        putExtra(
                                            "JewishMonth",
                                            jewishDateInfo.jewishCalendar.jewishMonth
                                        )
                                        putExtra("JewishYear", jewishDateInfo.jewishCalendar.jewishYear)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        if (currentText.imageAttachment == HighlightString.ImageAttachment.MENORAH) {
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
