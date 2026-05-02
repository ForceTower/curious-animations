package dev.forcetower.music.ui

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import dev.forcetower.music.R

/**
 * Single source of truth for lyrics typography + palette.
 *
 * Every lyrics view reads sizes / colors / fonts from here instead of touching
 * `R.color.*` / `R.dimen.*` directly. Phase 1 only carries what the line view
 * actually consumes (text size, active/inactive color, optional typeface).
 * Later phases extend this with sweep colors, glow shadow params, scale curves,
 * background-vocal text size, etc.
 */
data class LyricsStylesheet(
    val textSizeSp: Float,
    @param:ColorInt val colorActive: Int,
    @param:ColorInt val colorInactive: Int,
    val typeface: Typeface? = null,
) {
    companion object {
        fun appleDefault(context: Context): LyricsStylesheet = LyricsStylesheet(
            textSizeSp = 34f,
            colorActive = ContextCompat.getColor(context, R.color.lyrics_text),
            colorInactive = ContextCompat.getColor(context, R.color.lyrics_text_dim),
            typeface = ResourcesCompat.getFont(context, R.font.bold),
        )
    }
}