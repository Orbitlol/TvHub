package com.hearth.launcher

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

/** The Inputs panel: HDMI / AV / TV sources, D-pad friendly. */
object InputsDialog {

    private class Action(val label: String, val run: () -> Boolean)

    fun show(activity: Activity) {
        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog)
        val meter = activity.resources.displayMetrics.density
        val pad = { value: Int -> (value * meter + 0.5f).toInt() }

        val panel = LinearLayout(activity)
        panel.orientation = LinearLayout.VERTICAL
        panel.clipChildren = false
        panel.setPadding(pad(28), pad(24), pad(28), pad(24))
        panel.background = ContextCompat.getDrawable(activity, R.drawable.bg_dialog)

        val title = TextView(activity)
        title.text = activity.getString(R.string.inputs_title)
        title.textSize = 20f
        title.setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
        panel.addView(title)

        val subtitle = TextView(activity)
        subtitle.text = activity.getString(R.string.inputs_subtitle)
        subtitle.textSize = 12f
        subtitle.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
        subtitle.setPadding(0, pad(4), 0, pad(16))
        panel.addView(subtitle)

        val actions = ArrayList<Action>()
        for (source in Inputs.list(activity)) {
            actions.add(Action(source.label) { Inputs.switchTo(activity, source) })
        }
        actions.add(Action(activity.getString(R.string.inputs_menu)) { Inputs.openMenu(activity) })

        var first: View? = null
        for (chunk in actions.chunked(4)) {
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            row.clipChildren = false
            row.setPadding(0, 0, 0, pad(10))

            for (action in chunk) {
                val card = card(activity, action.label, pad) {
                    if (action.run()) {
                        dialog.dismiss()
                    } else {
                        Toast.makeText(activity, R.string.inputs_failed, Toast.LENGTH_SHORT).show()
                    }
                }
                if (first == null) first = card
                row.addView(card)
            }
            panel.addView(row)
        }

        dialog.setContentView(panel)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(pad(660), ViewGroup.LayoutParams.WRAP_CONTENT)
        first?.requestFocus()
    }

    private fun card(activity: Activity, label: String, pad: (Int) -> Int, onClick: () -> Unit): View {
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.isFocusable = true
        card.isClickable = true
        card.setPadding(pad(8), pad(12), pad(8), pad(10))
        card.background = ContextCompat.getDrawable(activity, R.drawable.tile_bg)
        Ui.attach(card)

        val icon = ImageView(activity)
        icon.setImageResource(R.drawable.ic_inputs)
        card.addView(icon, LinearLayout.LayoutParams(pad(28), pad(28)))

        val text = TextView(activity)
        text.text = label
        text.textSize = 13f
        text.maxLines = 2
        text.ellipsize = TextUtils.TruncateAt.END
        text.gravity = Gravity.CENTER
        text.setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
        text.setPadding(0, pad(8), 0, 0)
        card.addView(text, LinearLayout.LayoutParams(pad(118), ViewGroup.LayoutParams.WRAP_CONTENT))

        val params = LinearLayout.LayoutParams(pad(134), ViewGroup.LayoutParams.WRAP_CONTENT)
        params.marginEnd = pad(8)
        card.layoutParams = params

        card.setOnClickListener { onClick() }
        return card
    }
}
