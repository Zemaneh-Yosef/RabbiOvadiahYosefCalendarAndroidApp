package com.ej.rovadiahyosefcalendar.classes

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.verticalScrollbar(
	state: LazyListState,
	width: Dp = 4.dp,
	color: Color = Color.Gray.copy(alpha = 0.7f),
	topPadding: Dp = 0.dp,
	bottomPadding: Dp = 0.dp,
): Modifier = composed {
	drawWithContent {
		drawContent()

		val totalItems = state.layoutInfo.totalItemsCount
		if (totalItems == 0) return@drawWithContent

		val visibleItems = state.layoutInfo.visibleItemsInfo
		if (visibleItems.isEmpty()) return@drawWithContent

		val topPx = topPadding.toPx()
		val bottomPx = bottomPadding.toPx()
		val drawableHeight = size.height - topPx - bottomPx

		val estimatedItemHeight = visibleItems.sumOf { it.size } / visibleItems.size.toFloat()
		val estimatedTotalHeight = estimatedItemHeight * totalItems

		if (estimatedTotalHeight <= drawableHeight) return@drawWithContent

		val scrollbarHeight = (drawableHeight / estimatedTotalHeight * drawableHeight)
			.coerceIn(40f, drawableHeight)

		val scrollProgress = (state.firstVisibleItemIndex * estimatedItemHeight +
				state.firstVisibleItemScrollOffset) / (estimatedTotalHeight - drawableHeight)

		val scrollbarOffsetY = topPx + scrollProgress * (drawableHeight - scrollbarHeight)

		clipRect(top = topPx, bottom = size.height - bottomPx) {
			drawRoundRect(
				color = color,
				topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
				size = Size(width.toPx(), scrollbarHeight),
				cornerRadius = CornerRadius(width.toPx() / 2)
			)
		}
	}
}