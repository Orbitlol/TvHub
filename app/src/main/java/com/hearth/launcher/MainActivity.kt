package com.hearth.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Hearth: a quiet, offline TV launcher. D-pad only, plain Views. */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepo

    private lateinit var scrollView: NestedScrollView
    private lateinit var wallpaperView: View
    private lateinit var greetingView: TextView
    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private lateinit var favoritesEmpty: TextView
    private lateinit var sectionRecent: View
    private lateinit var rvFavorites: RecyclerView
    private lateinit var rvRecent: RecyclerView
    private lateinit var rvAll: RecyclerView

    private lateinit var favoritesAdapter: AppAdapter
    private lateinit var recentAdapter: AppAdapter
    private lateinit var allAdapter: AppAdapter

    private var apps: List<AppEntry> = emptyList()
    private var refreshQueued = false

    private val handler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())
    private lateinit var timeFormat: java.text.DateFormat

    private val clockTick = object : Runnable {
        override fun run() {
            showTime()
            showGreeting()
            handler.postDelayed(this, 20_000L)
        }
    }

    private val restoreTask = Runnable {
        if (currentFocus == null) {
            val restored = focusByKey(prefs.lastFocus()) ||
                focusFirst(rvFavorites) ||
                focusFirst(rvRecent) ||
                focusFirst(rvAll)
            if (!restored && allAdapter.itemCount > 0) {
                findViewById<View>(R.id.tileWifi).requestFocus()
            }
        }
    }

    private val homeTask = Runnable {
        scrollView.smoothScrollTo(0, 0)
        if (!focusFirst(rvFavorites)) findViewById<View>(R.id.tileWifi).requestFocus()
    }

    private val refreshTask = Runnable {
        refreshQueued = false
        loadApps()
    }

    private val packageWatcher = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            queueRefresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        repo = AppRepo(this)
        timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        setContentView(R.layout.activity_main)
        bindViews()
        setupRows()
        setupTiles()
        applyWallpaper()
        showTime()
        showGreeting()
    }

    override fun onResume() {
        super.onResume()
        // Respect the user's 12h / 24h system choice.
        timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        handler.removeCallbacks(clockTick)
        handler.post(clockTick)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageWatcher, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        loadApps()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockTick)
        try {
            unregisterReceiver(packageWatcher)
        } catch (e: Exception) {
            // Already unregistered; nothing to do.
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Home pressed again: back to the top, never leave the launcher.
        handler.removeCallbacks(homeTask)
        handler.postDelayed(homeTask, 80L)
    }

    override fun onBackPressed() {
        // A launcher never exits on Back; just stay exactly where we are.
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        repo.shutdown()
        super.onDestroy()
    }

    private fun bindViews() {
        scrollView = findViewById(R.id.scroll)
        wallpaperView = findViewById(R.id.wallpaper)
        greetingView = findViewById(R.id.greeting)
        clockView = findViewById(R.id.clock)
        dateView = findViewById(R.id.date)
        favoritesEmpty = findViewById(R.id.favoritesEmpty)
        sectionRecent = findViewById(R.id.sectionRecent)
        rvFavorites = findViewById(R.id.rvFavorites)
        rvRecent = findViewById(R.id.rvRecent)
        rvAll = findViewById(R.id.rvAll)
    }

    private fun setupRows() {
        favoritesAdapter = AppAdapter(false, ::openApp, { showAppMenu(it) }, { prefs.setLastFocus(it.key) })
        recentAdapter = AppAdapter(false, ::openApp, { showAppMenu(it) }, { prefs.setLastFocus(it.key) })
        allAdapter = AppAdapter(true, ::openApp, { showAppMenu(it) }, { prefs.setLastFocus(it.key) })

        rvFavorites.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rvFavorites.adapter = favoritesAdapter
        rvRecent.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rvRecent.adapter = recentAdapter
        rvAll.layoutManager = GridLayoutManager(this, gridColumns())
        rvAll.adapter = allAdapter
    }

    private fun gridColumns(): Int {
        val metrics = resources.displayMetrics
        val widthDp = metrics.widthPixels / metrics.density
        return ((widthDp - 112f) / 168f).toInt().coerceIn(3, 7)
    }

    private fun setupTiles() {
        setupTile(R.id.tileWifi, R.drawable.ic_wifi, R.string.tile_wifi) {
            openSettings(Settings.ACTION_WIFI_SETTINGS)
        }
        setupTile(R.id.tileInputs, R.drawable.ic_inputs, R.string.tile_inputs) {
            showInputs()
        }
        setupTile(R.id.tileDisplay, R.drawable.ic_display, R.string.tile_display) {
            openSettings(Settings.ACTION_DISPLAY_SETTINGS)
        }
        setupTile(R.id.tileSound, R.drawable.ic_sound, R.string.tile_sound) {
            openSettings(Settings.ACTION_SOUND_SETTINGS)
        }
        setupTile(R.id.tileSettings, R.drawable.ic_settings, R.string.tile_settings) {
            openSettings(Settings.ACTION_SETTINGS)
        }
        setupTile(R.id.tileWallpaper, R.drawable.ic_wallpaper, R.string.tile_wallpaper) {
            showWallpaperPicker()
        }
    }

    private fun setupTile(id: Int, icon: Int, label: Int, onClick: () -> Unit) {
        val tile = findViewById<View>(id)
        tile.findViewById<ImageView>(R.id.tileIcon).setImageResource(icon)
        tile.findViewById<TextView>(R.id.tileLabel).setText(label)
        Ui.attach(tile)
        tile.setOnClickListener { onClick() }
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            toast(R.string.action_unavailable)
        }
    }

    private fun applyWallpaper() {
        wallpaperView.background = Wallpapers.background(prefs.wallpaper)
    }

    private fun showTime() {
        val now = Date()
        clockView.text = timeFormat.format(now)
        dateView.text = dateFormat.format(now)
    }

    private fun showGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        greetingView.setText(
            when {
                hour < 5 -> R.string.greet_night
                hour < 12 -> R.string.greet_morning
                hour < 17 -> R.string.greet_afternoon
                else -> R.string.greet_evening
            }
        )
    }

    private fun loadApps() {
        repo.load { loaded ->
            if (isFinishing || isDestroyed) return@load
            apps = loaded
            val byKey = loaded.associateBy { it.key }

            favoritesAdapter.submit(prefs.favorites().mapNotNull { byKey[it] })
            recentAdapter.submit(prefs.recents().mapNotNull { byKey[it] })
            allAdapter.submit(loaded)

            val noFavorites = favoritesAdapter.itemCount == 0
            rvFavorites.visibility = if (noFavorites) View.GONE else View.VISIBLE
            favoritesEmpty.visibility = if (noFavorites) View.VISIBLE else View.GONE
            sectionRecent.visibility = if (recentAdapter.itemCount == 0) View.GONE else View.VISIBLE

            if (currentFocus == null) restoreFocus()
        }
    }

    private fun openApp(entry: AppEntry) {
        prefs.addRecent(entry.key)
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.component = entry.component
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            toast(R.string.action_unavailable)
            return
        }
        refreshRecents()
    }

    private fun refreshRecents() {
        val byKey = apps.associateBy { it.key }
        recentAdapter.submit(prefs.recents().mapNotNull { byKey[it] })
        sectionRecent.visibility = if (recentAdapter.itemCount == 0) View.GONE else View.VISIBLE
    }

    private fun showAppMenu(entry: AppEntry) {
        Dialogs.appMenu(this, prefs, entry) { loadApps() }
    }

    private fun showWallpaperPicker() {
        Dialogs.wallpaperPicker(this, prefs) { applyWallpaper() }
    }

    private fun showInputs() {
        InputsDialog.show(this)
    }

    private fun restoreFocus() {
        handler.removeCallbacks(restoreTask)
        handler.postDelayed(restoreTask, 120L)
    }

    private fun focusByKey(key: String?): Boolean {
        if (key == null) return false
        for (row in arrayOf(rvFavorites, rvRecent, rvAll)) {
            for (index in 0 until row.childCount) {
                val child = row.getChildAt(index) ?: continue
                if (child.tag == key) return child.requestFocus()
            }
        }
        return false
    }

    private fun focusFirst(row: RecyclerView): Boolean {
        if (row.visibility != View.VISIBLE) return false
        val first = row.layoutManager?.findViewByPosition(0) ?: return false
        return first.requestFocus()
    }

    private fun queueRefresh() {
        if (refreshQueued) return
        refreshQueued = true
        handler.postDelayed(refreshTask, 400L)
    }

    private fun toast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
