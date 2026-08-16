package com.pathsathi.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripEntity::class,
        TravelerEntity::class,
        BudgetExpenseEntity::class,
        EmergencyContactEntity::class,
        MemoryEntryEntity::class,
        ChatMessageEntity::class,
        SavedPlaceEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun travelerDao(): TravelerDao
    abstract fun budgetDao(): BudgetDao
    abstract fun emergencyDao(): EmergencyDao
    abstract fun memoryDao(): MemoryDao
    abstract fun chatDao(): ChatDao
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pathsathi.db"
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
