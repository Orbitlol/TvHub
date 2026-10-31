package com.hearth.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Reads every app that has a LEANBACK_LAUNCHER or LAUNCHER intent.
 * All PackageManager work happens on a single background thread and results
 * are posted back to the main thread. Banners/icons are cached in memory.
 */
class AppRepo(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val iconCache = HashMap<String, Drawable?>()

    fun load(onReady: (List<AppEntry>) -> Unit) {
        io.execute {
            val apps = query()
            main.post { onReady(apps) }
        }
    }

    fun shutdown() {
        io.shutdownNow()
        main.removeCallbacksAndMessages(null)
    }

    private fun query(): List<AppEntry> {
        val found = LinkedHashMap<String, AppEntry>()
        val probes = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        )
        for (probe in probes) {
            val resolved = try {
                pm.queryIntentActivities(probe, 0)
            } catch (e: Exception) {
                emptyList<ResolveInfo>()
            }
            for (info in resolved) {
                val activity = info.activityInfo ?: continue
                if (activity.packageName == context.packageName) continue
                val component = ComponentName(activity.packageName, activity.name)
                val key = component.flattenToString()
                if (found.containsKey(key)) continue
                val label = try {
                    info.loadLabel(pm).toString()
                } catch (e: Exception) {
                    ""
                }
                found[key] = AppEntry(
                    key = key,
                    label = label.ifBlank { activity.packageName },
                    pkg = activity.packageName,
                    component = component,
                    icon = readIcon(key, component)
                )
            }
        }
        return found.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /** Banner first (nice on TV), then activity icon, then app icon. */
    private fun readIcon(key: String, component: ComponentName): Drawable? {
        if (iconCache.containsKey(key)) return iconCache[key]
        val art = try {
            pm.getActivityBanner(component) ?: pm.getActivityIcon(component)
        } catch (e: Exception) {
            try {
                pm.getApplicationIcon(component.packageName)
            } catch (inner: Exception) {
                null
            }
        }
        iconCache[key] = art
        return art
    }
}
