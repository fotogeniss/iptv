package com.prelude.iptv.ui.mobile.settings

internal object AccountCarouselPolicy {
    fun nextPage(current: Int, pageCount: Int): Int =
        if (pageCount <= 0) 0 else (current.coerceIn(0, pageCount - 1) + 1) % pageCount

    fun pageAfterSwipe(current: Int, dragX: Float, pageCount: Int, threshold: Float = 45f): Int {
        if (pageCount <= 0 || kotlin.math.abs(dragX) < threshold) return current.coerceAtLeast(0)
        return (current + if (dragX < 0f) 1 else -1).coerceIn(0, pageCount - 1)
    }
}
