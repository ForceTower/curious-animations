package dev.forcetower.music.model

import android.content.Context
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Parses an Apple Music syllable-lyrics JSON envelope into a list of [LyricLine].
 *
 *  - Word-timed TTML only (`itunes:timing="Word"`). Line-timed lyrics, agent
 *    slots, background vocals, and instrumental gaps are deferred.
 *  - Each `<p>` becomes one [LyricLine]; each `<span>` with `begin`/`end`
 *    attributes becomes one [Syllable].
 *  - Whitespace text events between spans set [Syllable.trailingSpace].
 *  - `<span ttm:role="x-bg">` wrappers (background vocals) are skipped.
 */
object TtmlLyrics {

    private const val TTM_NS = "http://www.w3.org/ns/ttml#metadata"

    fun fromAsset(context: Context, assetName: String): List<LyricLine> {
        val json = context.assets.open(assetName).use { it.readBytes().toString(StandardCharsets.UTF_8) }
        val ttml = extractTtml(json)
        return parseTtml(ttml)
    }

    private fun extractTtml(json: String): String {
        return JSONObject(json)
            .getJSONArray("data")
            .getJSONObject(0)
            .getJSONObject("attributes")
            .getString("ttmlLocalizations")
    }

    private fun parseTtml(xml: String): List<LyricLine> {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setInput(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)), "UTF-8")

        val out = mutableListOf<LyricLine>()
        var current: MutableList<Syllable>? = null
        var pendingText: StringBuilder? = null
        var spanStart = 0L
        var spanEnd = 0L
        var bgDepth = 0

        var ev = parser.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "p" -> {
                        current = mutableListOf()
                        pendingText = null
                    }
                    "span" -> {
                        val role = parser.getAttributeValue(TTM_NS, "role")
                        if (role != null && role.startsWith("x-bg")) {
                            bgDepth++
                        } else if (bgDepth == 0 && current != null) {
                            val begin = parser.getAttributeValue(null, "begin")
                            val end = parser.getAttributeValue(null, "end")
                            if (begin != null && end != null) {
                                spanStart = parseTime(begin)
                                spanEnd = parseTime(end)
                                pendingText = StringBuilder()
                            }
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    if (bgDepth == 0 && current != null) {
                        val chunk = parser.text
                        if (chunk != null) {
                            val pt = pendingText
                            if (pt != null) {
                                // Inside a syllable span — accumulate its glyph text.
                                pt.append(chunk)
                            } else if (current.isNotEmpty() && chunk.any { it.isWhitespace() }) {
                                // Between two spans: mark the previous syllable as trailing.
                                val lastIdx = current.size - 1
                                val prev = current[lastIdx]
                                if (!prev.trailingSpace) {
                                    current[lastIdx] = prev.copy(trailingSpace = true)
                                }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "span" -> {
                        val role = parser.getAttributeValue(TTM_NS, "role")
                        if (role != null && role.startsWith("x-bg")) {
                            if (bgDepth > 0) bgDepth--
                        } else {
                            val pt = pendingText
                            val cur = current
                            if (pt != null && cur != null) {
                                cur += Syllable(
                                    pt.toString(),
                                    spanStart,
                                    spanEnd,
                                    trailingSpace = false
                                )
                                pendingText = null
                            }
                        }
                    }
                    "p" -> {
                        val cur = current
                        if (!cur.isNullOrEmpty()) out += LyricLine(cur)
                        current = null
                        pendingText = null
                    }
                }
            }
            ev = parser.next()
        }
        return out
    }

    /**
     * Parses a TTML time value into milliseconds. Apple's TTML uses two formats:
     *  - plain seconds with millisecond precision: `"7.926"`
     *  - `M:SS.sss` or `H:MM:SS.sss`: `"1:02.581"`, `"1:23:45.678"`
     */
    internal fun parseTime(value: String): Long {
        if (value.isEmpty()) return 0L
        val parts = value.split(":")
        val seconds = when (parts.size) {
            1 -> parts[0].toDouble()
            2 -> parts[0].toInt() * 60.0 + parts[1].toDouble()
            3 -> parts[0].toInt() * 3600.0 + parts[1].toInt() * 60.0 + parts[2].toDouble()
            else -> throw IllegalArgumentException("Unrecognized TTML time: $value")
        }
        return (seconds * 1000.0).roundToLong()
    }
}