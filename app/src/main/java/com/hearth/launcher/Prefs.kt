package com.hearth.launcher

import android.content.Context

/** Tiny SharedPreferences wrapper. Lists are stored as newline separated keys. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("hearth", Context.MODE_PRIVATE)

    var wallpaper: Int
        get() = sp.getInt(KEY_WALLPAPER, 0)
        set(value) {
            sp.edit().putInt(KEY_WALLPAPER, value).apply()
        }

    fun lastFocus(): String? = sp.getString(KEY_LAST_FOCUS, null)

    fun setLastFocus(key: String) {
        sp.edit().putString(KEY_LAST_FOCUS, key).apply()
    }

    fun favorites(): List<String> = read(KEY_FAVORITES)

    fun isFavorite(key: String): Boolean = favorites().contains(key)

    fun addFavorite(key: String) {
        val list = favorites().toMutableList()
        if (!list.contains(key)) list.add(key)
        write(KEY_FAVORITES, list)
    }

    fun removeFavorite(key: String) {
        val list = favorites().toMutableList()
        list.remove(key)
        write(KEY_FAVORITES, list)
    }

    fun recents(): List<String> = read(KEY_RECENTS)

    fun addRecent(key: String) {
        val list = recents().toMutableList()
        list.remove(key)
        list.add(0, key)
        while (list.size > MAX_RECENTS) list.removeAt(list.size - 1)
        write(KEY_RECENTS, list)
    }

    private fun read(name: String): List<String> {
        val raw = sp.getString(name, null)
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }
    }

    private fun write(name: String, list: List<String>) {
        sp.edit().putString(name, list.joinToString("\n")).apply()
    }

    private companion object {
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_LAST_FOCUS = "last_focus"
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENTS = "recents"
        const val MAX_RECENTS = 12
    }
}
