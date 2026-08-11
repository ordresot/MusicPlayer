package com.tushar.voidplayer.utils

data class LrcLine(
    val timestampMs: Long,
    val text: String
)

object LrcParser {
    private val timeRegex = Regex("""\[(\d{2}):(\d{2})(?:[.:](\d{2,3}))?\]""")

    fun parse(lrcContent: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        if (lrcContent.isBlank()) return emptyList()

        lrcContent.lineSequence().forEach { line ->
            val matches = timeRegex.findAll(line).toList()
            if (matches.isNotEmpty()) {
                val lyricText = line.replace(timeRegex, "").trim()
                for (match in matches) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val msFraction = match.groupValues[3]
                    
                    val ms = when (msFraction.length) {
                        2 -> (msFraction.toLongOrNull() ?: 0L) * 10
                        3 -> msFraction.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                    if (lyricText.isNotEmpty()) {
                        lines.add(LrcLine(totalMs, lyricText))
                    }
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    fun getCurrentLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        for (i in lines.indices.reversed()) {
            if (positionMs >= lines[i].timestampMs) {
                return i
            }
        }
        return 0
    }
}
