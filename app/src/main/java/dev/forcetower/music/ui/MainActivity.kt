package dev.forcetower.music.ui

import android.media.MediaPlayer
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
    private var player: MediaPlayer? = null

    private val lines: List<LyricLine> by lazy { loadLines() }

    private val tick = object : Runnable {
        override fun run() {
            val p = player ?: return
            setProgressMs(p.currentPosition.toLong())
            if (p.isPlaying) handler.postDelayed(this, 16L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.lyrics_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LyricsAdapter(lines, LyricsStylesheet.appleDefault(this))
        recyclerView.adapter = adapter

        startPlayback()
    }

    override fun onPause() {
        super.onPause()
        player?.takeIf { it.isPlaying }?.pause()
        handler.removeCallbacks(tick)
    }

    override fun onResume() {
        super.onResume()
        player?.let { p ->
            if (!p.isPlaying && p.currentPosition < p.duration) {
                p.start()
                handler.post(tick)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tick)
        player?.release()
        player = null
    }

    private fun startPlayback() {
        try {
            val afd = assets.openFd(SONG_ASSET_AUDIO)
            player = MediaPlayer().apply {
                afd.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                setOnPreparedListener {
                    it.start()
                    handler.post(tick)
                }
                setOnCompletionListener {
                    handler.removeCallbacks(tick)
                    setProgressMs(it.duration.toLong())
                }
                prepareAsync()
            }
        } catch (e: IOException) {
            Timber.e(e, "Failed to start playback for $SONG_ASSET_AUDIO")
        }
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
            TtmlLyrics.fromAsset(this, SONG_ASSET_LYRICS)
        } catch (e: IOException) {
            Timber.e(e, "Failed to load $SONG_ASSET_LYRICS")
            emptyList()
        }
    }

    companion object {
        private const val SONG_ASSET_LYRICS = "twinkle.json"
        private const val SONG_ASSET_AUDIO = "twinkle.mp3"
    }
}
