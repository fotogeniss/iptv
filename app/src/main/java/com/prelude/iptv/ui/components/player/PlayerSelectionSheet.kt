package com.prelude.iptv.ui.components.player

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Reusable player menu that becomes a right-side DPAD panel on TV and a
 * touch-first bottom sheet on phones. It intentionally contains no playback
 * logic; callers provide labels and callbacks.
 */
class PlayerSelectionSheet(
    context: Context,
    private val television: Boolean,
    private val onClosed: () -> Unit = {}
) : FrameLayout(context) {

    data class Option(
        val label: String,
        val selected: Boolean = false,
        val enabled: Boolean = true,
        val action: () -> Unit
    )

    private val scrim = View(context)
    private val panel = LinearLayout(context)
    private val titleView = TextView(context)
    private val optionsColumn = LinearLayout(context)

    val isShowing: Boolean
        get() = visibility == View.VISIBLE

    init {
        visibility = View.GONE
        isClickable = true
        isFocusable = true
        clipChildren = false

        scrim.setBackgroundColor(Color.parseColor("#8A000000"))
        scrim.setOnClickListener { dismiss() }
        addView(
            scrim,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        panel.orientation = LinearLayout.VERTICAL
        // Consume touches inside the sheet itself so they never fall through to the
        // fullscreen scrim/video surface. This is especially important on phones.
        panel.isClickable = true
        panel.isFocusable = false
        panel.setPadding(dp(if (television) 28 else 20), dp(if (television) 74 else 16), dp(if (television) 28 else 20), dp(24))
        panel.background = PremiumPlayerStyle.rounded(
            context,
            color = PremiumPlayerStyle.SURFACE_STRONG,
            radiusDp = if (television) 0 else 24,
            strokeColor = PremiumPlayerStyle.DIVIDER
        )

        titleView.setTextColor(Color.WHITE)
        titleView.textSize = if (television) 23f else 20f
        titleView.typeface = Typeface.DEFAULT_BOLD
        titleView.setPadding(0, 0, 0, dp(16))
        panel.addView(titleView)

        optionsColumn.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(
                optionsColumn,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
        panel.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val lp = if (television) {
            LayoutParams(dp(410), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END)
        } else {
            val maxHeight = (resources.displayMetrics.heightPixels * 0.72f).toInt()
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, minOf(dp(420), maxHeight), Gravity.BOTTOM)
        }
        if (!television) lp.setMargins(dp(8), 0, dp(8), 0)
        addView(panel, lp)
    }

    fun show(title: String, options: List<Option>) {
        titleView.text = title
        optionsColumn.removeAllViews()
        options.forEachIndexed { index, option ->
            val row = TextView(context).apply {
                text = buildString {
                    append(option.label)
                    if (option.selected) append("   ✓")
                }
                setTextColor(
                    Color.parseColor(
                        when {
                            !option.enabled -> "#66FFFFFF"
                            else -> PremiumPlayerStyle.TEXT_PRIMARY
                        }
                    )
                )
                textSize = if (television) 17f else 16f
                typeface = if (option.selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = PremiumPlayerStyle.rounded(
                    context,
                    color = if (option.selected) "#26FFFFFF" else "#00111115",
                    radiusDp = 11,
                    strokeColor = null,
                    strokeDp = 0
                )
                isEnabled = option.enabled
                isClickable = option.enabled
                isFocusable = television && option.enabled
                isFocusableInTouchMode = false
                setOnClickListener {
                    // Apply the player operation before removing the clicked row.
                    // Removing the row first could race the VLC track switch with
                    // the sheet's onClosed callback on some devices.
                    option.action()
                    dismiss()
                }
                if (television) {
                    PremiumPlayerStyle.applyFocus(
                        view = this,
                        context = context,
                        oval = false,
                        normalColor = if (option.selected) "#26FFFFFF" else "#00111115",
                        scale = 1.015f
                    )
                }
            }
            optionsColumn.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(6)
                }
            )
            if (television && (option.selected || index == 0)) {
                row.post { if (isShowing) row.requestFocus() }
            }
        }
        visibility = View.VISIBLE
        bringToFront()
        if (television) {
            optionsColumn.post {
                val selected = (0 until optionsColumn.childCount)
                    .map { optionsColumn.getChildAt(it) }
                    .firstOrNull { it.isEnabled && it is TextView && it.typeface?.isBold == true }
                    ?: (0 until optionsColumn.childCount)
                        .map { optionsColumn.getChildAt(it) }
                        .firstOrNull { it.isEnabled }
                selected?.requestFocus()
            }
        }
    }

    fun dismiss() {
        if (!isShowing) return
        visibility = View.GONE
        optionsColumn.removeAllViews()
        onClosed()
    }

    private fun dp(value: Int): Int = PremiumPlayerStyle.dp(context, value)
}
