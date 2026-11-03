package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_tracks")
data class MusicTrackEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val category: String,
    val durationMs: Long,
    val addedAtMs: Long,
    val lastPlayedAtMs: Long = 0L,
    val isFavorite: Boolean = false
)
