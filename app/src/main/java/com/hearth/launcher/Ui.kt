package com.hearth.launcher

import android.content.Context
import android.view.View
import android.view.animation.DecelerateInterpolator

/** Shared focus animation + small view helpers. */
object Ui {

    private const val FOCUS_SCALE = 1.08f
    private const val DURATION_MS = 150L

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    /** Smooth 1.08 scale + soft shadow, no layout re-measure. */
    fun animate(view: View, focused: Boolean) {
        view.animate().cancel()
        view.animate()
            .scaleX(if (focused) FOCUS_SCALE else 1f)
            .scaleY(if (focused) FOCUS_SCALE else 1f)
            .setDuration(DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        view.elevation = if (focused) 18f * view.resources.displayMetrics.density else 0f
    }

    fun attach(view: View) {
        view.setOnFocusChangeListener { v, focused -> animate(v, focused) }
    }
}
