package com.tushar.voidplayer.model

import androidx.compose.ui.graphics.Color

data class AiCategory(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val gradientColors: List<Color>,
    val songs: List<Song>
)
