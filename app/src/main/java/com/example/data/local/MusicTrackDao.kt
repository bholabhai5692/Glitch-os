package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicTrackDao {
    @Query("SELECT * FROM music_tracks ORDER BY title COLLATE NOCASE")
    fun observeTracks(): Flow<List<MusicTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<MusicTrackEntity>)

    @Query("UPDATE music_tracks SET category = :category WHERE uri = :uri")
    suspend fun updateCategory(uri: String, category: String)

    @Query("UPDATE music_tracks SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavorite(uri: String, isFavorite: Boolean)

    @Query("UPDATE music_tracks SET lastPlayedAtMs = :playedAtMs WHERE uri = :uri")
    suspend fun updateLastPlayed(uri: String, playedAtMs: Long)

    @Query("DELETE FROM music_tracks WHERE uri = :uri")
    suspend fun deleteTrack(uri: String)
}
