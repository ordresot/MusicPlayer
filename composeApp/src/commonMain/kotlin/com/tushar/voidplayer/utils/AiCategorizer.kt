package com.tushar.voidplayer.utils

import androidx.compose.ui.graphics.Color
import com.tushar.voidplayer.model.AiCategory
import com.tushar.voidplayer.model.Song

object AiCategorizer {

    fun categorize(songs: List<Song>): List<AiCategory> {
        if (songs.isEmpty()) return emptyList()

        val categories = mutableListOf<AiCategory>()

        // 1. Night Vibes & Chill
        val nightKeywords = listOf("night", "sleep", "dark", "moon", "midnight", "dream", "rain", "lofi", "chill", "slow", "quiet", "star", "shadow", "calm")
        val nightSongs = songs.filter { song ->
            val text = "${song.title} ${song.artist} ${song.album}".lowercase()
            nightKeywords.any { text.contains(it) }
        }
        if (nightSongs.isNotEmpty()) {
            categories.add(
                AiCategory(
                    id = "night_vibes",
                    title = "Night Vibes & Lo-Fi",
                    description = "Mellow, nocturnal, and chill atmospheric rhythms",
                    emoji = "🌙",
                    gradientColors = listOf(Color(0xFF2C3E50), Color(0xFF000000)),
                    songs = nightSongs
                )
            )
        }

        // 2. High Energy & Workout
        val energyKeywords = listOf("run", "fire", "fast", "energy", "hard", "rock", "drum", "gym", "power", "dance", "beat", "club", "party", "bass", "hyped", "speed", "fly")
        val energySongs = songs.filter { song ->
            val text = "${song.title} ${song.artist} ${song.album}".lowercase()
            energyKeywords.any { text.contains(it) }
        }
        if (energySongs.isNotEmpty()) {
            categories.add(
                AiCategory(
                    id = "high_energy",
                    title = "High Energy & Workout",
                    description = "Upbeat, motivating, and hard-hitting tempos",
                    emoji = "🔥",
                    gradientColors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
                    songs = energySongs
                )
            )
        }

        // 3. Deep Focus & Instrumental
        val focusKeywords = listOf("piano", "acoustic", "instrumental", "ambient", "study", "calm", "soft", "jazz", "classical", "meditation", "peace", "guitar", "soundtrack", "theme")
        val focusSongs = songs.filter { song ->
            val text = "${song.title} ${song.artist} ${song.album}".lowercase()
            focusKeywords.any { text.contains(it) }
        }
        if (focusSongs.isNotEmpty()) {
            categories.add(
                AiCategory(
                    id = "deep_focus",
                    title = "Deep Focus & Study",
                    description = "Acoustic and ambient textures for deep concentration",
                    emoji = "🎧",
                    gradientColors = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
                    songs = focusSongs
                )
            )
        }

        // 4. Romance & Heartfelt
        val romanceKeywords = listOf("love", "heart", "feel", "you", "kiss", "romantic", "forever", "miss", "together", "soul", "sweet", "angel")
        val romanceSongs = songs.filter { song ->
            val text = "${song.title} ${song.artist} ${song.album}".lowercase()
            romanceKeywords.any { text.contains(it) }
        }
        if (romanceSongs.isNotEmpty()) {
            categories.add(
                AiCategory(
                    id = "romance",
                    title = "Romance & Melodic",
                    description = "Emotional vocals and heartfelt harmonies",
                    emoji = "💖",
                    gradientColors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
                    songs = romanceSongs
                )
            )
        }

        // 5. Quick Hits (< 2.5 mins)
        val quickHits = songs.filter { it.duration in 1..150000 }
        if (quickHits.isNotEmpty()) {
            categories.add(
                AiCategory(
                    id = "quick_hits",
                    title = "Quick Hits & Singles",
                    description = "Fast-paced punchy tracks under 2.5 minutes",
                    emoji = "⚡",
                    gradientColors = listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
                    songs = quickHits
                )
            )
        }

        // 6. Extended & Epics (> 4.5 mins)
        val epics = songs.filter { it.duration >= 270000 }
        if (epics.isNotEmpty()) {
            categories.add(
                AiCategory(
                    id = "epics",
                    title = "Extended & Masterpieces",
                    description = "Longer journeys over 4.5 minutes",
                    emoji = "🎼",
                    gradientColors = listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)),
                    songs = epics
                )
            )
        }

        // 7. Top Artist Spotlights
        val topArtists = songs.groupBy { it.artist }
            .filter { it.key != "Unknown Artist" && it.value.size >= 2 }
            .entries.sortedByDescending { it.value.size }
            .take(3)

        topArtists.forEachIndexed { i, entry ->
            categories.add(
                AiCategory(
                    id = "artist_${entry.key.hashCode()}",
                    title = "Artist Spotlight: ${entry.key}",
                    description = "Complete collection of ${entry.value.size} tracks by ${entry.key}",
                    emoji = "🎙️",
                    gradientColors = when (i % 3) {
                        0 -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                        1 -> listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
                        else -> listOf(Color(0xFF141E30), Color(0xFF243B55))
                    },
                    songs = entry.value
                )
            )
        }

        // Fallback: If metadata didn't match specific keywords, provide a Smart Mix
        if (categories.isEmpty() && songs.isNotEmpty()) {
            categories.add(
                AiCategory(
                    id = "smart_mix",
                    title = "AI Daily Mix",
                    description = "Curated mix from your local music collection",
                    emoji = "✨",
                    gradientColors = listOf(Color(0xFF654ea3), Color(0xFFeaafc8)),
                    songs = songs.shuffled()
                )
            )
        }

        return categories
    }
}
