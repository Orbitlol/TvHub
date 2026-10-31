package com.hearth.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri

/** A TV source (HDMI 1, AV, TV ...) discovered from the TV input framework. */
data class InputSource(val id: String, val label: String)

/**
 * TV inputs are vendor territory: Android has no universal "switch input" API,
 * so we layer the options that TVs actually understand:
 *
 *  1. the standard TIF passthrough link
 *     content://android.media.tv/passthrough/<inputId>  (MediaTek / TCL / Hisense / MiTV Live TV)
 *  2. the TV's own source panel — Xiaomi's ExternalSourceActivity first, then the
 *     AOSP VIEW_INPUTS action, then the known OEM input panels.
 *
 * Everything is guarded: a missing handler just means "not available".
 */
object Inputs {

    private val XIAOMI_PANEL = ComponentName(
        "com.xiaomi.mitv.tvplayer",
        "com.xiaomi.mitv.tvplayer.ExternalSourceActivity"
    )

    private const val VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS"

    private val menuActions = arrayOf(
        VIEW_INPUTS,
        "com.xiaomi.mitv.settings.INPUTSOURCE_POPUP",
        "com.mitv.tvhome.atv.app.tv.INPUTSOURCE_POPUP"
    )

    private val menuComponents = arrayOf(
        ComponentName("com.mediatek.wwtv.tvcenter", "com.mediatek.wwtv.tvcenter.nav.inputpanel.InputsPanelActivity"),
        ComponentName("com.google.android.tvlauncher", "com.google.android.tvlauncher.inputs.InputsPanelActivity"),
        ComponentName("com.vt.tv.input", "com.vt.tv.input.view.activity.MainActivity")
    )

    /** Real inputs only; the TV's own menu is always offered as an extra card. */
    fun list(context: Context): List<InputSource> {
        val sources = ArrayList<InputSource>()
        val inputs = try {
            val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            manager?.tvInputList ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        for (info in inputs) {
            try {
                val id = info.id
                if (id.isNullOrEmpty()) continue
                val label = info.loadLabel(context).toString().trim()
                if (label.isEmpty()) continue
                sources.add(InputSource(id, label))
            } catch (e: Exception) {
                // Skip inputs that cannot describe themselves.
            }
        }
        return sources.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /** Switch to one source, or fall back to the TV's own input menu. */
    fun switchTo(context: Context, source: InputSource): Boolean {
        if (source.id.isNotEmpty()) {
            val uri: Uri? = try {
                TvContract.buildChannelUriForPassthroughInput(source.id)
            } catch (e: Exception) {
                null
            }
            if (uri != null && tryStart(context, Intent(Intent.ACTION_VIEW, uri))) return true
        }
        return openMenu(context)
    }

    /** Open the TV's built-in source picker. */
    fun openMenu(context: Context): Boolean {
        if (startComponent(context, XIAOMI_PANEL)) return true
        for (action in menuActions) {
            val intent = Intent(action).addCategory(Intent.CATEGORY_DEFAULT)
            if (tryStart(context, intent)) return true
        }
        for (component in menuComponents) {
            if (startComponent(context, component)) return true
        }
        return false
    }

    private fun startComponent(context: Context, component: ComponentName): Boolean {
        val intent = Intent()
        intent.component = component
        return tryStart(context, intent)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
