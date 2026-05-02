package dev.forcetower.music.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.forcetower.music.R
import dev.forcetower.music.model.LyricLine

class LyricsAdapter(
    private val lines: List<LyricLine>,
    @Suppress("unused") private val style: LyricsStylesheet,
) : RecyclerView.Adapter<LyricsAdapter.LineHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineHolder {
        val view = LyricsLineSimpleView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = parent.resources.getDimensionPixelSize(
                    R.dimen.lyrics_line_between_line_spacing
                )
            }
        }
        return LineHolder(view)
    }

    override fun onBindViewHolder(holder: LineHolder, position: Int) {
        holder.view.setLine(lines[position])
        holder.view.setActiveState(active = false)
    }

    override fun getItemCount(): Int = lines.size

    class LineHolder(val view: LyricsLineSimpleView) : RecyclerView.ViewHolder(view)
}