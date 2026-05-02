package dev.forcetower.music.model

/**
 * One line of lyrics: an ordered list of [Syllable]s plus the line's start/end
 * window (computed from the first and last syllable).
 */
class LyricLine(syllables: List<Syllable>) {
    val syllables: List<Syllable> = syllables.toList()
    val startMs: Long = syllables.first().startMs
    val endMs: Long = syllables.last().endMs

    /** Joined text of all syllables, with literal spaces where [Syllable.trailingSpace] was set. */
    fun text(): String = buildString {
        for (s in syllables) {
            append(s.text)
            if (s.trailingSpace) append(' ')
        }
    }
}