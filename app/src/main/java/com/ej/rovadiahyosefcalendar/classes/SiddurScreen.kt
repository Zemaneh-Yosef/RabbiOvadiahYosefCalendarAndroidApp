package com.ej.rovadiahyosefcalendar.classes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.SpannableString
import android.util.TypedValue
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.CenterAlignedTopAppBar
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

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// 1. Define a type for our scroll event lambda
typealias ScrollToPosition = (Int) -> Unit

@Composable
fun SiddurScreenEntry(
	siddurContent: List<HighlightString>,
	jewishDateInfo: JewishDateInfo,
	siddurTitle: String,
	categoryNames: List<String>,
	categoryPositions: List<Int>,
	onNavigateBack: () -> Unit,
	onScrollRequested: (ScrollToPosition) -> Unit,
) {
	SiddurScreen(
		siddurContent = siddurContent,
		jewishDateInfo = jewishDateInfo,
		siddurTitle = siddurTitle,
		categoryNames = categoryNames,
		categoryPositions = categoryPositions,
		onNavigateBack = onNavigateBack,
		onScrollRequested = onScrollRequested,
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

/**
 * The Material 3 focus ring that the rest of the app gets from DpadSupport (which can't see inside Compose): a 3dp ring in colorSecondary
 * with a thin colorSurface ring inside it. It shows when this element or something inside it has the focus, and only while the user is
 * using a D-pad or a keyboard (on TV), never on touch.
 *
 * @param cornerRadius the ring's corners, or null for a round ring (icon buttons)
 */
@Composable
private fun Modifier.dpadFocusRing(cornerRadius: Dp? = 12.dp, onFocusChange: (Boolean) -> Unit = {}): Modifier {
	val inputModeManager = LocalInputModeManager.current
	var hasFocus by remember { mutableStateOf(false) }
	val ringColor = getThemeColor(com.google.android.material.R.attr.colorSecondary)
	val innerColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
	return this
		.onFocusChanged {
			if (hasFocus != it.hasFocus) {
				hasFocus = it.hasFocus
				onFocusChange(it.hasFocus)
			}
		}
		.drawWithContent {
			drawContent()
			if (hasFocus && inputModeManager.inputMode == InputMode.Keyboard) {
				val outer = 3.dp.toPx()
				val inner = 1.5.dp.toPx()
				val radius = cornerRadius?.toPx() ?: (size.minDimension / 2)
				for ((inset, width, color) in listOf(Triple(outer / 2, outer, ringColor), Triple(outer + inner / 2, inner, innerColor))) {
					drawRoundRect(
						color = color,
						topLeft = Offset(inset, inset),
						size = Size(size.width - 2 * inset, size.height - 2 * inset),
						cornerRadius = CornerRadius((radius - inset).coerceAtLeast(0f)),
						style = Stroke(width)
					)
				}
			}
		}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiddurScreen(
	siddurContent: List<HighlightString>,
	jewishDateInfo: JewishDateInfo,
	siddurTitle: String,
	categoryNames: List<String>,
	categoryPositions: List<Int>,
	onNavigateBack: () -> Unit,
	onScrollRequested: (ScrollToPosition) -> Unit,
) {
	val context = LocalContext.current
	val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }

	var textSize by remember { mutableFloatStateOf(getMigratedTextSize(sharedPreferences)) }
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

	// State for the category dropdown menu
	var showCategoryMenu by remember { mutableStateOf(false) }

	val defaultBackgroundColor = getThemeColor(android.R.attr.colorBackground)
	val defaultTextColor: Color = getThemeColor(android.R.attr.textColorPrimary)

	Scaffold(
		containerColor = defaultBackgroundColor,
		topBar = {
			CenterAlignedTopAppBar(
				title = {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text(siddurTitle)
						Text(
							text = stringResource(AppR.string.short_app_name),
							style = MaterialTheme.typography.bodySmall
						)
					}
				},
				navigationIcon = {
					IconButton(onClick = onNavigateBack, modifier = Modifier.dpadFocusRing(cornerRadius = null)) {
						Icon(
							painter = painterResource(id = AppR.drawable.baseline_arrow_back_24),
							contentDescription = null
						)
					}
				},
				actions = {
					if (categoryNames.isNotEmpty()) {
						IconButton(onClick = { showCategoryMenu = true }, modifier = Modifier.dpadFocusRing(cornerRadius = null)) {
							Icon(
								painter = painterResource(id = AppR.drawable.baseline_menu_24),
								contentDescription = null
							)
						}
						DropdownMenu(
							expanded = showCategoryMenu,
							onDismissRequest = { showCategoryMenu = false },
                            modifier = Modifier.background(defaultBackgroundColor)
						) {
							categoryNames.forEachIndexed { index, name ->
								DropdownMenuItem(
									text = { Text(name) },
                                    colors = MenuItemColors(
                                        textColor = defaultTextColor,
                                        leadingIconColor = defaultTextColor,
                                        trailingIconColor = defaultTextColor,
                                        disabledTextColor = defaultTextColor,
                                        disabledLeadingIconColor = defaultTextColor,
                                        disabledTrailingIconColor = defaultTextColor
                                    ),
									onClick = {
										showCategoryMenu = false
										if (index in categoryPositions.indices) {
											coroutineScope.launch {
												listState.animateScrollToItem(categoryPositions[index])
											}
										}
									}
								)
							}
						}
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = getThemeColor(android.R.attr.windowBackground),
					titleContentColor = getThemeColor(android.R.attr.textColorPrimary),
					navigationIconContentColor = getThemeColor(android.R.attr.textColorPrimary),
					actionIconContentColor = getThemeColor(android.R.attr.textColorPrimary),
				)
			)
		},
		bottomBar = {
			SiddurBottomBar(
				currentTextSize = textSize,
				isJustified = isJustified,
				onTextSizeChange = { newSize ->
					textSize = newSize
					sharedPreferences.edit {
						putFloat("TextSize", newSize)
					}
				},
				onJustifyClick = {
					isJustified = !isJustified
					sharedPreferences.edit { putBoolean("isJustified", isJustified) }
				}
			)
		}
	) { innerPadding ->
		var focusedRow by remember { mutableIntStateOf(-1) }
		LazyColumn(
			modifier = Modifier
				.fillMaxWidth()
				// Padding, not contentPadding: a list that reaches up behind the top bar isn't "below" its buttons, so the D-pad's down
				// key would skip the whole prayer and land on the text size slider. The bars are opaque, so it looks the same.
				.padding(innerPadding)
				// D-pad: a row can be taller than the screen (a long paragraph). Up/down first scroll through the rest of the focused
				// row, half a screen at a time, and only then move the focus to the next row, so no text is skipped.
				.onPreviewKeyEvent { event ->
					val down = event.key == Key.DirectionDown
					if (event.type != KeyEventType.KeyDown || (!down && event.key != Key.DirectionUp)) {
						return@onPreviewKeyEvent false
					}
					val info = listState.layoutInfo
					val row = info.visibleItemsInfo.firstOrNull { it.index == focusedRow } ?: return@onPreviewKeyEvent false
					val visibleEnd = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding// below the top bar, above the bottom bar
					val step = visibleEnd * 0.5f
					val hidden = if (down) row.offset + row.size - visibleEnd else -row.offset
					if (hidden <= 0) {
						return@onPreviewKeyEvent false// the whole row is on screen, let the focus move on
					}
					coroutineScope.launch {
						listState.animateScrollBy(if (down) minOf(step, hidden.toFloat()) else -minOf(step, hidden.toFloat()))
					}
					true
				}
				.verticalScrollbar(state = listState),
			state = listState
		) {
			itemsIndexed(siddurContent) { index, currentText ->
				SiddurRow(
					onFocusChange = { focused ->
						if (focused) {
							focusedRow = index
						} else if (focusedRow == index) {
							focusedRow = -1
						}
					},
					currentText = currentText,
					textSize = textSize,
					isJustified = isJustified,
					fontPreference = fontPreference,
					jewishDateInfo = jewishDateInfo,
					context = context
				)
			}
		}
	}
}

fun getMigratedTextSize(sharedPreferences: SharedPreferences): Float {
    return try {// Try reading it as a Float first (for new or already migrated users)
        sharedPreferences.getFloat("TextSize", 24.0f)
    } catch (e: ClassCastException) {// The old value is still an Int, which caused the crash. Read it as an Int.
        val oldIntValue = sharedPreferences.getInt("TextSize", 24)
        val floatValue = oldIntValue.toFloat()
        // Migrate the value to a Float so we don't hit this catch block again
        sharedPreferences.edit { putFloat("TextSize", floatValue) }

        floatValue
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
        val focusManager = LocalFocusManager.current
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val valueRange = 18f..45f
        Slider(
            value = currentTextSize,
            onValueChange = onTextSizeChange,
            valueRange = valueRange,
            modifier = Modifier
                .weight(1f)
                .dpadFocusRing()
                // D-pad: the slider would use every arrow key to change its value, so the focus could never leave it. Up and down
                // move the focus instead, and left/right move on once the slider is at that end.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    val atStart = currentTextSize <= valueRange.start
                    val atEnd = currentTextSize >= valueRange.endInclusive
                    when (event.key) {
                        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up).let { true }
                        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down).let { true }
                        Key.DirectionRight -> if ((!isRtl && atEnd) || (isRtl && atStart)) focusManager.moveFocus(FocusDirection.Right) else false
                        Key.DirectionLeft -> if ((!isRtl && atStart) || (isRtl && atEnd)) focusManager.moveFocus(FocusDirection.Left) else false
                        else -> false
                    }
                }
        )
        IconButton(onClick = onJustifyClick, modifier = Modifier.dpadFocusRing(cornerRadius = null)) {
            Icon(
                painter = painterResource(id = if (isJustified) AppR.drawable.baseline_format_align_justify_24 else AppR.drawable.baseline_format_align_right_24),
                contentDescription = stringResource(AppR.string.justify_text),
                tint = LocalContentColor.current
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

	// Unwrapped heading: may go below 0 / above 360 so the animation always
	// takes the shortest path across the 359° -> 0° boundary.
	var unwrappedDegree by remember { mutableFloatStateOf(0f) }

	DisposableEffect(Unit) {
		val accelerometerReading = FloatArray(3)
		val magnetometerReading = FloatArray(3)
		val rotationMatrix = FloatArray(9)
		val orientationAngles = FloatArray(3)
		val window = LinkedList<Double>()
		var lastFilteredDegree = Float.NaN // previous raw (0..360) heading

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
					val filtered = filtrate(azimuthDegrees)

					if (lastFilteredDegree.isNaN()) {
						unwrappedDegree = filtered
					} else {
						// Shortest signed difference between the two headings
						var delta = filtered - lastFilteredDegree
						if (delta > 180f) delta -= 360f
						if (delta < -180f) delta += 360f
						unwrappedDegree += delta
					}
					lastFilteredDegree = filtered
				}
			}

			override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
				when (accuracy) {
					SensorManager.SENSOR_STATUS_UNRELIABLE, SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
						Toast.makeText(context, AppR.string.sensor_accuracy_is_low, Toast.LENGTH_SHORT).show()
					}
					SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM, SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
						Toast.makeText(context, AppR.string.sensor_accuracy_is_okay, Toast.LENGTH_SHORT).show()
					}
					SensorManager.SENSOR_STATUS_NO_CONTACT -> {
						Toast.makeText(context, AppR.string.no_sensor_found, Toast.LENGTH_SHORT).show()
					}
				}
			}
		}

		sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
		sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)

		onDispose {
			sensorManager.unregisterListener(sensorListener)
		}
	}

	val animatedDegree by animateFloatAsState(
		targetValue = -unwrappedDegree,
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioMediumBouncy,
			stiffness = Spring.StiffnessLow
		),
		label = "CompassRotation"
	)

	if (accelerometer != null && magnetometer != null) {// need both
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
					.fillMaxWidth(0.8f)
					.aspectRatio(1f)
					.rotate(animatedDegree)
			)
		}
	}
}

@Composable
private fun SiddurRow(
	onFocusChange: (Boolean) -> Unit,
	currentText: HighlightString,
	textSize: Float,
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

	val text = remember(currentText.content, textSize) {
		SpannableString(currentText.content).asAnnotatedString().annotatedString
	}

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
		(currentTextType == HighlightString.StringType.INFO || currentTextType == HighlightString.StringType.INVERSE_INFO) ->
			if (isNightMode) Color.DarkGray else Color.LightGray
		currentText.isHighlighted -> colorResource(if (isNightMode) AppR.color.goldenrod else AppR.color.mainly_BLUE)
		else -> Color.Transparent
	}

	val isInfo = currentTextType == HighlightString.StringType.INFO
	val isInverseInfo = currentTextType == HighlightString.StringType.INVERSE_INFO
	var isInfoExpanded by remember { mutableStateOf(false) }
	val onRowClick: () -> Unit = {
		if (isInfo || isInverseInfo) {
			isInfoExpanded = !isInfoExpanded
		} else if (text.text == "Open Sefaria Siddur" || text.text == "פתח את סידור ספריה") {
			val browserIntent = Intent(
				Intent.ACTION_VIEW,
				"https://www.sefaria.org/Siddur_Edot_HaMizrach?tab=contents".toUri()
			)
			Utils.startActivitySafely(context, browserIntent)
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

	Column(modifier = Modifier
		.background(rowBackgroundColor)
		.dpadFocusRing(cornerRadius = 0.dp, onFocusChange = onFocusChange)
		// D-pad (TV): every row takes the focus, so up/down read through the prayer row by row (the list scrolls along, see
		// SiddurScreen), and OK does what tapping the row does
		.onKeyEvent { event ->
			val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
			if (isOk && event.type == KeyEventType.KeyUp) {
				onRowClick()
				true
			} else {
				false
			}
		}
		.focusable()
	) {
		val isParagraph = currentText.bigWordsStart > 0
		val isCategory = currentTextType == HighlightString.StringType.CATEGORY
		val isInstruction = currentTextType == HighlightString.StringType.INSTRUCTION

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

		val textDirection = remember(currentText.content) {
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
				TextDirection.Rtl
			} else {
				TextDirection.Ltr
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

		// Rest of the composable is unchanged from your original.
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
							onClick = onRowClick
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