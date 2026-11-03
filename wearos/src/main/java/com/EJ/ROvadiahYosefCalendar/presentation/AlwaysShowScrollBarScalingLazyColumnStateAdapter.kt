package com.EJ.ROvadiahYosefCalendar.presentation

import androidx.compose.runtime.State
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType.Companion.ItemCenter
import androidx.wear.compose.foundation.lazy.ScalingLazyListItemInfo
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility

class AlwaysShowScrollBarScalingLazyColumnStateAdapter(
    private val state: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    private val viewportHeightPx: State<Int?>,
    private val anchorType: ScalingLazyListAnchorType = ItemCenter,
) : PositionIndicatorState {
    override val positionFraction: Float
        get() {
            if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0.0f
            val decimalFirstItemIndex = decimalFirstItemIndex()
            val decimalLastItemIndex = decimalLastItemIndex()
            val decimalLastItemIndexDistanceFromEnd = state.layoutInfo.totalItemsCount - decimalLastItemIndex
            val denominator = decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd
            return if (denominator == 0.0f) 0.0f else (decimalFirstItemIndex / denominator).safeFraction()
        }

    override fun sizeFraction(scrollableContainerSizePx: Float): Float =
        if (state.layoutInfo.totalItemsCount == 0) {
            1.0f
        } else {
            ((decimalLastItemIndex() - decimalFirstItemIndex()) /
                    state.layoutInfo.totalItemsCount.toFloat()).safeFraction()
        }

    // always shown, which is the point of this adapter
    override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility =
        PositionIndicatorVisibility.Show

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? AlwaysShowScrollBarScalingLazyColumnStateAdapter)?.state == state
    }

    /**
     * Provide a float value that represents the index of the last visible list item in a scaling
     * lazy column. The value should be in the range from [n,n+1] for a given index n, where n is
     * the index of the last visible item and a value of n represents that only the very start|top
     * of the item is visible, and n+1 means that whole of the item is visible in the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    private fun decimalLastItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        val lastItemEndOffset = lastItem.startOffset(anchorType) + lastItem.size
        val viewportEndOffset = (viewportHeightPx.value ?: 0) / 2f
        val lastItemVisibleFraction = if (lastItem.size <= 0) {
            1f// a zero-height item has nothing to hide, count it as fully visible
        } else {
            (1f - ((lastItemEndOffset - viewportEndOffset) / lastItem.size)).coerceIn(0f, 1f)
        }
        return lastItem.index.toFloat() + lastItemVisibleFraction
    }

    /**
     * Provide a float value that represents the index of first visible list item in a scaling lazy
     * column. The value should be in the range from [n,n+1] for a given index n, where n is the
     * index of the first visible item and a value of n represents that all of the item is visible
     * in the viewport and a value of n+1 means that only the very end|bottom of the list item is
     * visible at the start|top of the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    private fun decimalFirstItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = state.layoutInfo.visibleItemsInfo.first()
        val firstItemStartOffset = firstItem.startOffset(anchorType)
        val viewportStartOffset = -((viewportHeightPx.value ?: 0) / 2f)
        val firstItemInvisibleFraction = if (firstItem.size <= 0) {
            0f
        } else {
            ((viewportStartOffset - firstItemStartOffset) / firstItem.size).coerceIn(0f, 1f)
        }
        return firstItem.index.toFloat() + firstItemInvisibleFraction
    }
}

/** NaN or infinity crashes PositionIndicator's animation, so clamp to a valid fraction. */
private fun Float.safeFraction(): Float =
    if (isNaN() || isInfinite()) 0f else coerceIn(0f, 1f)

internal fun ScalingLazyListItemInfo.startOffset(anchorType: ScalingLazyListAnchorType) =
    offset - if (anchorType == ItemCenter) {
        (size / 2f)
    } else {
        0f
    }
