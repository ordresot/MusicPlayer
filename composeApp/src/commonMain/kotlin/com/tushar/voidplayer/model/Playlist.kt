package com.tushar.voidplayer.model

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long> = emptyList()
)
