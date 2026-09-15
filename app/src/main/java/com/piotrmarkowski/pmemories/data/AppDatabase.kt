package com.piotrmarkowski.pmemories.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        MediaItemEntity::class,
        CaptionEntity::class,
        OverlayItemEntity::class,
        TripEntity::class,
        StopEntity::class
    ],
    // 15.09.2026 — Etap 7: two new entities (TripEntity/StopEntity). No
    // migration written — pre-release, no real users on any store yet, only
    // local test/dev installs. `fallbackToDestructiveMigration()` wipes and
    // recreates on schema mismatch instead of crashing those installs; a
    // real migration path is needed before this ships to actual testers.
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun captionDao(): CaptionDao
    abstract fun overlayItemDao(): OverlayItemDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pmemories.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
