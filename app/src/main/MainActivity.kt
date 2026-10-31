package com.example.tvhub

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import org.json.JSONObject
import java.net.URL

const val TMDB_KEY = "" // paste TMDB v3 key; blank = offline fallback

data class Item(val title: String, val poster: String?)
data class Row(val name: String, val items: List<Item>)
class VH(v: View) : RecyclerView.ViewHolder(v)

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun fetch(path: String): List<Item> {
    val c = URL("https://api.themoviedb.org/3/$path?api_key=$TMDB_KEY").openConnection()
    c.connectTimeout = 5000; c.readTimeout = 5000
    val a = JSONObject(c.getInputStream().bufferedReader().readText()).getJSONArray("results")
    return List(a.length()) {
        val o = a.getJSONObject(it)
        Item(o.optString("title", o.optString("name")),
            if (o.isNull("poster_path")) null else "https://image.tmdb.org/t/p/w342" + o.getString("poster_path"))
    }
}

fun load(ctx: Context): List<Row> {
    val a = JSONObject(ctx.assets.open("fallback.json").bufferedReader().readText()).getJSONArray("rows")
    val rows = List(a.length()) {
        val o = a.getJSONObject(it); val i = o.getJSONArray("items")
        Row(o.getString("name"), List(i.length()) { k -> Item(i.getString(k), null) })
    }
    if (TMDB_KEY.isBlank()) return rows
    val api = mapOf("Trending Movies" to "trending/movie/week", "Popular Series" to "tv/popular", "Top Rated Movies" to "movie/top_rated")
    return rows.map { r -> api[r.name]?.let { runCatching { fetch(it) }.getOrNull() }?.let { Row(r.name, it) } ?: r }
}

class CardAdapter(val items: List<Item>) : RecyclerView.Adapter<VH>() {
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
        val c = p.context
        val r = c.dp(10).toFloat()
        val border = GradientDrawable().apply { cornerRadius = r; setStroke(c.dp(3), Color.WHITE) }
        val f = FrameLayout(c).apply {
            layoutParams = RecyclerView.LayoutParams(c.dp(170), c.dp(250)).apply { setMargins(c.dp(8), c.dp(14), c.dp(8), c.dp(14)) }
            isFocusable = true
            background = GradientDrawable().apply { cornerRadius = r }
            clipToOutline = true
            setOnFocusChangeListener { v, h ->
                v.animate().scaleX(if (h) 1.1f else 1f).scaleY(if (h) 1.1f else 1f).setDuration(120).start()
                v.translationZ = if (h) 20f else 0f
                v.foreground = if (h) border else null
            }
            addView(ImageView(c).apply { scaleType = ImageView.ScaleType.CENTER_CROP })
            addView(TextView(c).apply {
                setTextColor(Color.WHITE); textSize = 15f; maxLines = 2
                setBackgroundColor(0xAA000000.toInt()); setPadding(c.dp(8), c.dp(6), c.dp(8), c.dp(6))
                layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)
            })
        }
        return VH(f)
    }
    override fun onBindViewHolder(h: VH, i: Int) {
        val m = items[i]; val f = h.itemView as FrameLayout
        (f.background as GradientDrawable).setColor(Color.HSVToColor(floatArrayOf((m.title.hashCode() and 255) * 1.4f, .5f, .35f)))
        (f.getChildAt(1) as TextView).text = m.title
        val iv = f.getChildAt(0) as ImageView
        if (m.poster != null) iv.load(m.poster) else iv.setImageDrawable(null)
        f.setOnClickListener { Toast.makeText(f.context, "Play: ${m.title}", Toast.LENGTH_SHORT).show() }
    }
}

class RowAdapter(val rows: List<Row>) : RecyclerView.Adapter<VH>() {
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
        val c = p.context
        val tv = TextView(c).apply {
            setTextColor(Color.WHITE); textSize = 22f; setTypeface(null, Typeface.BOLD)
            setPadding(c.dp(8), c.dp(16), 0, 0)
        }
        val rv = RecyclerView(c).apply {
            layoutManager = LinearLayoutManager(c, RecyclerView.HORIZONTAL, false)
            clipChildren = false; clipToPadding = false
        }
        return VH(LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL; clipChildren = false
            layoutParams = RecyclerView.LayoutParams(-1, -2)
            addView(tv); addView(rv)
        })
    }
    override fun onBindViewHolder(h: VH, i: Int) {
        val g = h.itemView as ViewGroup
        (g.getChildAt(0) as TextView).text = rows[i].name
        (g.getChildAt(1) as RecyclerView).adapter = CardAdapter(rows[i].items)
    }
}

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setBackgroundColor(0xFF0B0B12.toInt())
            clipChildren = false; clipToPadding = false
            setPadding(dp(40), dp(24), dp(40), dp(24))
        }
        setContentView(list)
        Thread {
            val rows = load(this)
            runOnUiThread {
                list.adapter = RowAdapter(rows)
                list.postDelayed({ list.getChildAt(0)?.requestFocus() }, 300)
            }
        }.start()
    }
}