package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        OfflineQueueEntity::class,
        MusicTrackEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun offlineQueueDao(): OfflineQueueDao
    abstract fun musicTrackDao(): MusicTrackDao

    companion object {
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS user_auth")
                db.execSQL("DROP TABLE IF EXISTS scheduler_settings")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS music_tracks (
                        uri TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        category TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        addedAtMs INTEGER NOT NULL,
                        lastPlayedAtMs INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: VideoDatabase? = null

        fun getDatabase(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "glitch_video_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
