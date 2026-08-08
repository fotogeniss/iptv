package com.prelude.iptv.ui.components.player

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.viewMotionDuration
import com.prelude.iptv.ui.design.viewMotionScale

/** Shared visual tokens for the native PlayerActivity overlay. */
object PremiumPlayerStyle {
    const val BACKGROUND = "#050507"
    const val SURFACE = "#D9141418"
    const val SURFACE_STRONG = "#F2111115"
    const val SURFACE_SOFT = "#481A1A20"
    const val TEXT_PRIMARY = "#FFFFFFFF"
    const val TEXT_SECONDARY = "#C7FFFFFF"
    const val TEXT_MUTED = "#8FFFFFFF"
    const val DIVIDER = "#24FFFFFF"
    const val FOCUS = "#FFF4F4F6"
    const val FOCUS_CONTENT = "#FF101014"
    const val ACCENT = "#E50914"

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun rounded(
        context: Context,
        color: String = SURFACE,
        radiusDp: Int = 14,
        strokeColor: String? = DIVIDER,
        strokeDp: Int = 1
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, radiusDp).toFloat()
        setColor(Color.parseColor(color))
        if (strokeColor != null && strokeDp > 0) {
            setStroke(dp(context, strokeDp), Color.parseColor(strokeColor))
        }
    }

    fun circle(
        context: Context,
        color: String = SURFACE_SOFT,
        strokeColor: String? = DIVIDER,
        strokeDp: Int = 1
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor(color))
        if (strokeColor != null && strokeDp > 0) {
            setStroke(dp(context, strokeDp), Color.parseColor(strokeColor))
        }
    }

    fun applyFocus(
        view: View,
        context: Context,
        oval: Boolean,
        normalColor: String = SURFACE_SOFT,
        focusedColor: String = FOCUS,
        scale: Float = Motion.TvActionScale
    ) {
        view.setOnFocusChangeListener { target, hasFocus ->
            target.background = if (oval) {
                circle(
                    context = context,
                    color = if (hasFocus) focusedColor else normalColor,
                    strokeColor = if (hasFocus) FOCUS else DIVIDER,
                    strokeDp = if (hasFocus) 2 else 1
                )
            } else {
                rounded(
                    context = context,
                    color = if (hasFocus) focusedColor else normalColor,
                    radiusDp = 12,
                    strokeColor = if (hasFocus) FOCUS else DIVIDER,
                    strokeDp = if (hasFocus) 2 else 1
                )
            }
            updateContentTint(target, hasFocus)
            target.alpha = if (hasFocus) 1f else 0.96f
            target.animate()
                .scaleX(if (hasFocus) viewMotionScale(context, scale) else 1f)
                .scaleY(if (hasFocus) viewMotionScale(context, scale) else 1f)
                .setDuration(viewMotionDuration(context, Motion.Focus))
                .start()
        }
    }

    private fun updateContentTint(view: View, focused: Boolean) {
        val color = Color.parseColor(if (focused) FOCUS_CONTENT else TEXT_PRIMARY)
        when (view) {
            is ImageView -> view.setColorFilter(color)
            is TextView -> view.setTextColor(color)
            is ViewGroup -> {
                for (index in 0 until view.childCount) {
                    updateContentTint(view.getChildAt(index), focused)
                }
            }
        }
    }
}
