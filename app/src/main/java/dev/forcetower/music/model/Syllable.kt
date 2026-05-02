package dev.forcetower.music.model

/**
 * One timed unit inside a lyric line. Always a whole word in for now; later
 * phases may split a word into multiple syllables when the source TTML does.
 *
 * [trailingSpace] mirrors the literal whitespace text event between `<span>`s
 * in Apple's TTML — when true, this syllable is followed by a space before the
 * next one. Always false for the last syllable in a line.
 */
data class Syllable(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val trailingSpace: Boolean,
)