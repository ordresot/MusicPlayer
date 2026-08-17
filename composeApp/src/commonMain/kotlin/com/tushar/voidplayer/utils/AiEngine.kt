package com.tushar.voidplayer.utils

import androidx.compose.ui.graphics.Color
import com.tushar.voidplayer.model.AiCategory
import com.tushar.voidplayer.model.Song

data class AiInsightsData(
    val dominantVibe: String,
    val vibeEmoji: String,
    val personalityTitle: String,
    val personalityDescription: String,
    val totalHoursMinutes: String,
    val topArtist: String,
    val recommendedEq: String,
    val vibeBreakdown: List<Pair<String, Int>> // Vibe Name to Percentage
)

object AiEngine {

    fun generateInsights(songs: List<Song>): AiInsightsData {
        if (songs.isEmpty()) {
            return AiInsightsData(
                dominantVibe = "Silence",
                vibeEmoji = "🌌",
                personalityTitle = "The Clean Slate",
                personalityDescription = "Load your audio files to unlock Void AI insights.",
                totalHoursMinutes = "0m",
                topArtist = "None",
                recommendedEq = "Flat / Studio",
                vibeBreakdown = emptyList()
            )
        }

        val totalDurationMs = songs.sumOf { it.duration }
        val hours = totalDurationMs / (1000 * 60 * 60)
        val mins = (totalDurationMs / (1000 * 60)) % 60
        val durationStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val categories = AiCategorizer.categorize(songs)
        val categoryCounts = categories.map { it.title to it.songs.size }.sortedByDescending { it.second }
        val totalCategoryTracks = categoryCounts.sumOf { it.second }.coerceAtLeast(1)

        val breakdown = categoryCounts.take(4).map {
            it.first to ((it.second * 100) / totalCategoryTracks)
        }

        val topCategory = categories.maxByOrNull { it.songs.size }

        val dominantVibe = topCategory?.title ?: "Eclectic Vibe"
        val vibeEmoji = topCategory?.emoji ?: "✨"

        val (personality, description, eq) = when {
            dominantVibe.contains("Night", ignoreCase = true) -> Triple(
                "The Midnight Wanderer",
                "Your library leans heavily toward mellow, nocturnal, and atmospheric sounds.",
                "Warm Studio / Ambient"
            )
            dominantVibe.contains("Energy", ignoreCase = true) -> Triple(
                "The High-Drive Dynamo",
                "Packed with high-tempo beats, rock power, and motivating rhythms.",
                "Heavy Bass Boost"
            )
            dominantVibe.contains("Focus", ignoreCase = true) -> Triple(
                "The Deep Focus Architect",
                "Curated for deep concentration, acoustic textures, and productive flow.",
                "Acoustic Clarity"
            )
            dominantVibe.contains("Romance", ignoreCase = true) -> Triple(
                "The Soulful Romantic",
                "Rich with emotional vocals, heartfelt harmonies, and expressive melodies.",
                "Vocal Pop Boost"
            )
            else -> Triple(
                "The Sonic Explorer",
                "A diverse, balanced spectrum of genres, tempos, and musical landscapes.",
                "Dynamic Normalization"
            )
        }

        val topArtist = songs.groupBy { it.artist }
            .filter { it.key != "Unknown Artist" }
            .maxByOrNull { it.value.size }?.key ?: "Various Artists"

        return AiInsightsData(
            dominantVibe = dominantVibe,
            vibeEmoji = vibeEmoji,
            personalityTitle = personality,
            personalityDescription = description,
            totalHoursMinutes = durationStr,
            topArtist = topArtist,
            recommendedEq = eq,
            vibeBreakdown = breakdown
        )
    }

    /**
     * AI DJ Flow: Intelligently calculates the next most harmonious track
     * based on mood keywords, tempo similarity, and artist affinity.
     */
    fun getAiDjNextSong(currentSong: Song, allSongs: List<Song>, queue: List<Song>): Song? {
        if (allSongs.size <= 1) return null
        val candidates = allSongs.filter { it.id != currentSong.id }

        // Find songs sharing keywords with current track
        val currentWords = "${currentSong.title} ${currentSong.artist} ${currentSong.album}".lowercase()
            .split(" ", "_", "-")
            .filter { it.length > 3 }

        val scored = candidates.map { song ->
            var score = 0
            val songText = "${song.title} ${song.artist} ${song.album}".lowercase()

            // 1. Keyword match
            for (word in currentWords) {
                if (songText.contains(word)) score += 3
            }

            // 2. Artist affinity
            if (song.artist.equals(currentSong.artist, ignoreCase = true) && song.artist != "Unknown Artist") {
                score += 4
            }

            // 3. Duration similarity (within 45 seconds)
            val durationDiff = kotlin.math.abs(song.duration - currentSong.duration)
            if (durationDiff < 45000) {
                score += 2
            }

            // 4. Favor songs not already in recent queue
            if (queue.takeLast(3).none { it.id == song.id }) {
                score += 1
            }

            song to score
        }

        val bestMatches = scored.sortedByDescending { it.second }
        // Pick among top 3 matches with slight randomization for freshness
        val topPicks = bestMatches.take(3).map { it.first }
        return topPicks.randomOrNull() ?: candidates.randomOrNull()
    }
}
