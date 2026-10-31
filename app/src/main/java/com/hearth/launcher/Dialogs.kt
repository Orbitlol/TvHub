package com.hearth.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

/** App long-press menu + wallpaper picker. */
object Dialogs {

    fun appMenu(activity: Activity, prefs: Prefs, entry: AppEntry, onChanged: () -> Unit) {
        val pinned = prefs.isFavorite(entry.key)
        val actions = arrayOf(
            activity.getString(if (pinned) R.string.unpin else R.string.pin),
            activity.getString(R.string.app_info),
            activity.getString(R.string.uninstall)
        )
        AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(entry.label)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        if (pinned) prefs.removeFavorite(entry.key) else prefs.addFavorite(entry.key)
                        onChanged()
                    }
                    1 -> openDetails(activity, entry.pkg)
                    else -> uninstall(activity, entry.pkg)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun wallpaperPicker(activity: Activity, prefs: Prefs, onPicked: () -> Unit) {
        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog)
        val meter = activity.resources.displayMetrics.density
        val pad = { value: Int -> (value * meter + 0.5f).toInt() }

        val panel = LinearLayout(activity)
        panel.orientation = LinearLayout.VERTICAL
        panel.clipChildren = false
        panel.setPadding(pad(28), pad(24), pad(28), pad(26))
        panel.background = ContextCompat.getDrawable(activity, R.drawable.bg_dialog)

        val title = TextView(activity)
        title.text = activity.getString(R.string.wallpaper_title)
        title.textSize = 20f
        title.setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
        title.setPadding(0, 0, 0, pad(16))
        panel.addView(title)

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.clipChildren = false
        for (i in 0 until Wallpapers.count) {
            val item = LinearLayout(activity)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL
            item.isFocusable = true
            item.isClickable = true
            item.setPadding(pad(8), pad(8), pad(8), pad(8))
            item.background = ContextCompat.getDrawable(activity, R.drawable.tile_bg)
            Ui.attach(item)

            val swatch = View(activity)
            swatch.background = Wallpapers.swatch(i, pad(14).toFloat())
            swatch.layoutParams = LinearLayout.LayoutParams(pad(132), pad(76))
            item.addView(swatch)

            val name = TextView(activity)
            name.text = Wallpapers.names[i]
            name.textSize = 13f
            name.gravity = Gravity.CENTER
            name.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
            name.setPadding(0, pad(8), 0, 0)
            item.addView(name)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = pad(14)
            item.layoutParams = params

            item.setOnClickListener {
                prefs.wallpaper = i
                onPicked()
                dialog.dismiss()
            }
            row.addView(item)
        }
        panel.addView(row)

        dialog.setContentView(panel)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(pad(880), ViewGroup.LayoutParams.WRAP_CONTENT)
        row.getChildAt(prefs.wallpaper.coerceIn(0, Wallpapers.count - 1))?.requestFocus()
    }

    private fun openDetails(activity: Activity, pkg: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", pkg, null)
        launch(activity, intent)
    }

    private fun uninstall(activity: Activity, pkg: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.fromParts("package", pkg, null)
        launch(activity, intent)
    }

    private fun launch(activity: Activity, intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.action_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
