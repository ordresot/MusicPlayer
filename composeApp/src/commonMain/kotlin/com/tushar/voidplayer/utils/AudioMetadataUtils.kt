package com.tushar.voidplayer.utils

import com.tushar.voidplayer.model.Song

data class CodecInfo(
    val formatName: String,
    val isLossless: Boolean,
    val isHiRes: Boolean,
    val badgeLabel: String,
    val sampleRateEstimate: String,
    val bitrateEstimate: String
)

object AudioMetadataUtils {

    fun inspectSong(song: Song): CodecInfo {
        val uri = song.uri.lowercase()
        val title = song.title.lowercase()

        return when {
            uri.endsWith(".flac") || title.contains("flac") -> CodecInfo(
                formatName = "FLAC Lossless",
                isLossless = true,
                isHiRes = true,
                badgeLabel = "✨ Hi-Res FLAC",
                sampleRateEstimate = "24-bit / 96.0 kHz",
                bitrateEstimate = "~1411 kbps Lossless"
            )
            uri.endsWith(".wav") || title.contains("wav") -> CodecInfo(
                formatName = "WAV PCM",
                isLossless = true,
                isHiRes = true,
                badgeLabel = "✨ Hi-Res WAV",
                sampleRateEstimate = "16-bit / 44.1 kHz PCM",
                bitrateEstimate = "1411.2 kbps Uncompressed"
            )
            uri.endsWith(".m4a") || uri.endsWith(".aac") -> CodecInfo(
                formatName = "AAC / M4A",
                isLossless = false,
                isHiRes = false,
                badgeLabel = "🎵 AAC HD",
                sampleRateEstimate = "44.1 kHz Stereo",
                bitrateEstimate = "256 kbps VBR"
            )
            uri.endsWith(".ogg") || uri.endsWith(".opus") -> CodecInfo(
                formatName = "OGG / Opus",
                isLossless = false,
                isHiRes = false,
                badgeLabel = "🎵 OPUS HD",
                sampleRateEstimate = "48.0 kHz Stereo",
                bitrateEstimate = "192 kbps"
            )
            else -> CodecInfo(
                formatName = "MPEG Audio (MP3)",
                isLossless = false,
                isHiRes = false,
                badgeLabel = "🎵 320 kbps MP3",
                sampleRateEstimate = "44.1 kHz Stereo",
                bitrateEstimate = "320 kbps CBR"
            )
        }
    }
}
