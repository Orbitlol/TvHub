package com.hearth.launcher

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * Card list for one home row (or the all-apps grid).
 * Enter = open, hold Enter (>= 500ms) = menu, focus = scale + glow + remembered.
 */
class AppAdapter(
    private val grid: Boolean,
    private val onOpen: (AppEntry) -> Unit,
    private val onLongPress: (AppEntry) -> Unit,
    private val onFocus: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private var items: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) {
        val old = items
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = old.size
            override fun getNewListSize(): Int = list.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
                old[oldPos].key == list[newPos].key
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
                old[oldPos] == list[newPos]
        })
        items = list
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        val params = view.layoutParams as RecyclerView.LayoutParams
        params.width = if (grid) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            Ui.dp(parent.context, 152)
        }
        view.layoutParams = params
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewRecycled(holder: Holder) {
        holder.clearPending()
        super.onViewRecycled(holder)
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {

        private val icon: ImageView = view.findViewById(R.id.cardIcon)
        private val label: TextView = view.findViewById(R.id.cardLabel)
        private var pending: Runnable? = null
        private var menuArmed = false

        fun bind(entry: AppEntry) {
            itemView.tag = entry.key
            label.text = entry.label
            val art = entry.icon
            if (art != null) icon.setImageDrawable(art) else icon.setImageResource(R.drawable.ic_placeholder)

            clearPending()
            menuArmed = false

            // Armed after 500ms of holding; the menu opens on key release so no
            // stray key-up can land inside the dialog and pick an action.
            val armMenu = Runnable { menuArmed = true }
            pending = armMenu

            itemView.setOnClickListener { onOpen(entry) }
            itemView.setOnFocusChangeListener { v, focused ->
                Ui.animate(v, focused)
                if (focused) {
                    onFocus(entry)
                } else {
                    v.removeCallbacks(armMenu)
                    menuArmed = false
                }
            }
            itemView.setOnKeyListener { v, code, event ->
                handleKey(v, code, event, entry, armMenu)
            }
        }

        private fun handleKey(
            view: View,
            code: Int,
            event: KeyEvent,
            entry: AppEntry,
            armMenu: Runnable
        ): Boolean {
            val isEnter = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                code == KeyEvent.KEYCODE_ENTER ||
                code == KeyEvent.KEYCODE_NUMPAD_ENTER
            if (!isEnter) return false

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        menuArmed = false
                        view.postDelayed(armMenu, 500L)
                    } else if (!menuArmed) {
                        menuArmed = true
                        view.removeCallbacks(armMenu)
                    }
                }
                KeyEvent.ACTION_UP -> {
                    view.removeCallbacks(armMenu)
                    val showMenu = menuArmed
                    menuArmed = false
                    if (showMenu) onLongPress(entry) else onOpen(entry)
                }
                else -> return false
            }
            return true
        }

        fun clearPending() {
            pending?.let { itemView.removeCallbacks(it) }
        }
    }
}
