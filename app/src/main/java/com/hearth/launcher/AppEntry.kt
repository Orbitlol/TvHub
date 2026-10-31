package com.hearth.launcher

import android.content.ComponentName
import android.graphics.drawable.Drawable

/** One launchable app on the device. [key] is the flattened ComponentName. */
data class AppEntry(
    val key: String,
    val label: String,
    val pkg: String,
    val component: ComponentName,
    val icon: Drawable?
)
