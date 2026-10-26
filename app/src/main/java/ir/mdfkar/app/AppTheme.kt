package ir.mdfkar.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/** Centralized visual system for MDF-KAR. Change colors, borders and shapes here. */
object AppTheme {
    val BACKGROUND = Color.rgb(3, 12, 24)
    val BACKGROUND_TOP = Color.rgb(7, 22, 42)
    val CARD = Color.rgb(8, 25, 45)
    val FIELD = Color.rgb(13, 35, 60)
    val PRIMARY = Color.rgb(0, 112, 245)
    val ACCENT = Color.rgb(35, 166, 255)
    val TEXT = Color.rgb(239, 246, 255)
    val TEXT_SECONDARY = Color.rgb(176, 202, 230)
    val MUTED = Color.rgb(135, 164, 196)
    val CARD_BORDER = Color.rgb(28, 91, 143)
    val FIELD_BORDER = Color.rgb(43, 102, 153)

    fun rounded(context: Context, color: Int, radiusDp: Int, strokeColor: Int? = null, strokeDp: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp * context.resources.displayMetrics.density
            if (strokeColor != null && strokeDp > 0) {
                setStroke((strokeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1), strokeColor)
            }
        }

    fun heroBackground(context: Context): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.rgb(5, 19, 38), Color.rgb(8, 42, 78), Color.rgb(4, 15, 30))
    ).apply {
        cornerRadius = 22 * context.resources.displayMetrics.density
        setStroke((1 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1), ACCENT)
    }

    fun primaryButton(context: Context): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(Color.rgb(0, 145, 255), Color.rgb(0, 83, 220))
    ).apply {
        cornerRadius = 16 * context.resources.displayMetrics.density
        setStroke((1 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1), Color.rgb(75, 185, 255))
    }

    fun navBackground(context: Context): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Color.rgb(6, 25, 46), Color.rgb(3, 12, 24))
    ).apply {
        cornerRadius = 18 * context.resources.displayMetrics.density
        setStroke((1 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1), CARD_BORDER)
    }
}
