package com.pathsathi.app.data.repository

import com.pathsathi.app.data.db.AppDatabase
import com.pathsathi.app.data.db.BudgetExpenseEntity
import com.pathsathi.app.data.db.ChatMessageEntity
import com.pathsathi.app.data.db.EmergencyContactEntity
import com.pathsathi.app.data.db.MemoryEntryEntity
import com.pathsathi.app.data.db.SavedPlaceEntity
import com.pathsathi.app.data.db.TravelerEntity
import com.pathsathi.app.data.db.TripEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all local (offline) data.
 * Every read/write here works fully without network. Any future online/AI
 * layer must call through this repository rather than replace it, so the
 * app degrades gracefully to offline-saved data whenever the network layer
 * is unavailable.
 */
class PathSathiRepository(db: AppDatabase) {
    private val tripDao = db.tripDao()
    private val travelerDao = db.travelerDao()
    private val budgetDao = db.budgetDao()
    private val emergencyDao = db.emergencyDao()
    private val memoryDao = db.memoryDao()
    private val chatDao = db.chatDao()
    private val savedPlaceDao = db.savedPlaceDao()

    // Trips
    fun observeTrips(): Flow<List<TripEntity>> = tripDao.observeAll()
    fun observeActiveTrip(): Flow<TripEntity?> = tripDao.observeActiveTrip()
    suspend fun saveTrip(trip: TripEntity): Long = tripDao.upsert(trip)
    suspend fun updateTrip(trip: TripEntity) = tripDao.update(trip)
    suspend fun deleteTrip(trip: TripEntity) = tripDao.delete(trip)
    suspend fun getTrip(id: Long): TripEntity? = tripDao.getById(id)

    // Budget
    fun observeExpenses(tripId: Long): Flow<List<BudgetExpenseEntity>> = budgetDao.observeForTrip(tripId)
    fun observeExpensesForTraveler(tripId: Long, travelerId: Long): Flow<List<BudgetExpenseEntity>> = budgetDao.observeForTraveler(tripId, travelerId)
    fun observeTotalSpent(tripId: Long): Flow<Int> = budgetDao.observeTotalSpent(tripId)
    suspend fun addExpense(expense: BudgetExpenseEntity): Long = budgetDao.insert(expense)
    suspend fun deleteExpense(expense: BudgetExpenseEntity) = budgetDao.delete(expense)

    // Travelers (Phase 21 — group/family trip)
    fun observeTravelers(tripId: Long): Flow<List<TravelerEntity>> = travelerDao.observeForTrip(tripId)
    suspend fun saveTraveler(traveler: TravelerEntity): Long = travelerDao.upsert(traveler)
    suspend fun deleteTraveler(traveler: TravelerEntity) = travelerDao.delete(traveler)

    // Emergency
    fun observeEmergencyContacts(): Flow<List<EmergencyContactEntity>> = emergencyDao.observeAll()
    suspend fun saveEmergencyContact(contact: EmergencyContactEntity): Long = emergencyDao.upsert(contact)
    suspend fun deleteEmergencyContact(contact: EmergencyContactEntity) = emergencyDao.delete(contact)

    // Memory
    fun observeMemory(): Flow<List<MemoryEntryEntity>> = memoryDao.observeAll()
    fun observeMemoryForTrip(tripId: Long): Flow<List<MemoryEntryEntity>> = memoryDao.observeForTrip(tripId)
    suspend fun addMemory(entry: MemoryEntryEntity): Long = memoryDao.insert(entry)
    suspend fun deleteMemory(entry: MemoryEntryEntity) = memoryDao.delete(entry)

    // Chat
    fun observeChat(): Flow<List<ChatMessageEntity>> = chatDao.observeAll()
    suspend fun addChatMessage(message: ChatMessageEntity): Long = chatDao.insert(message)
    suspend fun clearChat() = chatDao.clearAll()

    // Saved places
    fun observeSavedPlaces(): Flow<List<SavedPlaceEntity>> = savedPlaceDao.observeAll()
    suspend fun savePlace(place: SavedPlaceEntity): Long = savedPlaceDao.upsert(place)
    suspend fun deleteSavedPlace(place: SavedPlaceEntity) = savedPlaceDao.delete(place)

    companion object {
        @Volatile private var INSTANCE: PathSathiRepository? = null
        fun getInstance(db: AppDatabase): PathSathiRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PathSathiRepository(db).also { INSTANCE = it }
            }
    }
}
