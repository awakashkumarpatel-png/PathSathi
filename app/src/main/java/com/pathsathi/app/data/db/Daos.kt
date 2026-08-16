package com.pathsathi.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActiveTrip(): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): TripEntity?
}

@Dao
interface BudgetDao {
    @Insert
    suspend fun insert(expense: BudgetExpenseEntity): Long

    @Delete
    suspend fun delete(expense: BudgetExpenseEntity)

    @Query("SELECT * FROM budget_expenses WHERE tripId = :tripId ORDER BY dateEpochMs DESC")
    fun observeForTrip(tripId: Long): Flow<List<BudgetExpenseEntity>>

    @Query("SELECT * FROM budget_expenses WHERE tripId = :tripId AND travelerId = :travelerId ORDER BY dateEpochMs DESC")
    fun observeForTraveler(tripId: Long, travelerId: Long): Flow<List<BudgetExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amountInr), 0) FROM budget_expenses WHERE tripId = :tripId")
    fun observeTotalSpent(tripId: Long): Flow<Int>
}

@Dao
interface TravelerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(traveler: TravelerEntity): Long

    @Delete
    suspend fun delete(traveler: TravelerEntity)

    @Query("SELECT * FROM travelers WHERE tripId = :tripId ORDER BY id ASC")
    fun observeForTrip(tripId: Long): Flow<List<TravelerEntity>>
}

@Dao
interface EmergencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: EmergencyContactEntity): Long

    @Delete
    suspend fun delete(contact: EmergencyContactEntity)

    @Query("SELECT * FROM emergency_contacts")
    fun observeAll(): Flow<List<EmergencyContactEntity>>
}

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(entry: MemoryEntryEntity): Long

    @Query("SELECT * FROM travel_memory ORDER BY dateEpochMs DESC")
    fun observeAll(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM travel_memory WHERE tripId = :tripId ORDER BY dateEpochMs DESC")
    fun observeForTrip(tripId: Long): Flow<List<MemoryEntryEntity>>
}

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM sathi_chat ORDER BY timestampEpochMs ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM sathi_chat")
    suspend fun clearAll()
}

@Dao
interface SavedPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: SavedPlaceEntity): Long

    @Delete
    suspend fun delete(place: SavedPlaceEntity)

    @Query("SELECT * FROM saved_places ORDER BY id DESC")
    fun observeAll(): Flow<List<SavedPlaceEntity>>
}
