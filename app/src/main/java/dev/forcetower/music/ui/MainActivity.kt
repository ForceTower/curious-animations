package dev.forcetower.music.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.forcetower.music.R
import dev.forcetower.music.model.LyricLine
import dev.forcetower.music.model.TtmlLyrics
import timber.log.Timber
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: LyricsAdapter
    private lateinit var recyclerView: RecyclerView

    private val lines: List<LyricLine> by lazy { loadLines() }

    private var playing = false
    // Current song position
    private var currentTimeMs = 0L
    // Song duration
    private var totalMs = 60000L
    private var lastTickAtMs = 0L

    private val tick = object : Runnable {
        override fun run() {
            if (!playing) return
            val now = System.currentTimeMillis()
            currentTimeMs += (now - lastTickAtMs)
            lastTickAtMs = now
            if (currentTimeMs >= totalMs) {
                currentTimeMs = totalMs
                pause()
            }
            setProgressMs(currentTimeMs)
            handler.postDelayed(this, 16L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.lyrics_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LyricsAdapter(lines, LyricsStylesheet.appleDefault(this))
        recyclerView.adapter = adapter

        play()
    }

    private fun play() {
        playing = true
        lastTickAtMs = System.currentTimeMillis()
        handler.post(tick)
    }

    private fun pause() {
        playing = false
        handler.removeCallbacks(tick)
    }

    private fun setProgressMs(currentTime: Long) {
        lines.forEachIndexed { index, line ->
            val active = line.startMs <= currentTime && line.endMs > currentTime
            val view = recyclerView.findViewHolderForAdapterPosition(index)?.itemView ?: return@forEachIndexed
            val casted = (view as LyricsLineSimpleView)
            if (casted.active != active) {
                if (active) view.runActivate() else casted.runDeactivate()
            }
        }
    }

    private fun loadLines(): List<LyricLine> {
        return try {
            TtmlLyrics.fromAsset(this, SONG_ASSET)
        } catch (e: IOException) {
            Timber.e(e, "Failed to load $SONG_ASSET")
            emptyList()
        }
    }

    companion object {
        private const val SONG_ASSET = "believer.json"
    }
}