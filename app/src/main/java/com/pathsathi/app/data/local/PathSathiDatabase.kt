package com.pathsathi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        EmergencyContactEntity::class,
        JournalEntryEntity::class,
        TrackingSessionEntity::class,
        TrackPointEntity::class,
        FavoriteEntity::class,
        TripPlanEntity::class,
        TripEntity::class,
        OfflineMapEntity::class,
        CheckInEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PathSathiDatabase : RoomDatabase() {
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun journalDao(): JournalDao
    abstract fun trackingDao(): TrackingDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun tripPlanDao(): TripPlanDao
    abstract fun tripDao(): TripDao
    abstract fun offlineMapDao(): OfflineMapDao
    abstract fun checkInDao(): CheckInDao

    companion object {
        @Volatile
        private var INSTANCE: PathSathiDatabase? = null

        fun getInstance(context: Context): PathSathiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PathSathiDatabase::class.java,
                    "pathsathi.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
