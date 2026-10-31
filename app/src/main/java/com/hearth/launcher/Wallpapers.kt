package com.hearth.launcher

import android.graphics.drawable.GradientDrawable

/** The five built-in soft gradients. The chosen index is stored in [Prefs]. */
object Wallpapers {

    val names = arrayOf("Aurora", "Dusk", "Ember", "Moss", "Slate")

    private val palettes = arrayOf(
        intArrayOf(0xFF0A1230.toInt(), 0xFF123B4D.toInt(), 0xFF1E5B62.toInt()),
        intArrayOf(0xFF0B0F22.toInt(), 0xFF201747.toInt(), 0xFF3B2360.toInt()),
        intArrayOf(0xFF121018.toInt(), 0xFF3A1F2E.toInt(), 0xFF5B3033.toInt()),
        intArrayOf(0xFF0B1512.toInt(), 0xFF143528.toInt(), 0xFF1F4A33.toInt()),
        intArrayOf(0xFF0C1220.toInt(), 0xFF1B2637.toInt(), 0xFF2C3A4E.toInt())
    )

    val count: Int get() = names.size

    fun background(index: Int): GradientDrawable = gradient(index, 0f)

    fun swatch(index: Int, radiusPx: Float): GradientDrawable = gradient(index, radiusPx)

    private fun gradient(index: Int, radius: Float): GradientDrawable {
        val colors = palettes[index.coerceIn(0, count - 1)]
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = radius
        }
    }
}
