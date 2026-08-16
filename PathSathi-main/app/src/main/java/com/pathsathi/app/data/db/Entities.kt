package com.pathsathi.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val destination: String,
    val days: Int,
    val budgetInr: Int,
    val travelers: Int,
    val tripType: String,
    val status: String, // PLANNED, ACTIVE, COMPLETED, CANCELLED
    val itineraryJson: String, // serialized List<ItineraryDay>
    val createdAtEpochMs: Long,
    val startedAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null,
    val currentDayIndex: Int = 0,
    val isGroupTrip: Boolean = false
)

/** Phase 21 — group/family trip data architecture: one row per traveler on a trip. */
@Entity(
    tableName = "travelers",
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tripId")]
)
data class TravelerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val isGroupLeader: Boolean = false
)

@Entity(
    tableName = "budget_expenses",
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TravelerEntity::class, parentColumns = ["id"], childColumns = ["travelerId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("tripId"), Index("travelerId")]
)
data class BudgetExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val category: String, // Travel, Stay, Food, Tickets, Activities, Shopping, Other
    val amountInr: Int,
    val note: String,
    val dateEpochMs: Long,
    /** Null = shared/group expense. Set = tracked against one traveler (Phase 21 individual tracking). */
    val travelerId: Long? = null
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val relation: String,
    val isTrusted: Boolean = true
)

@Entity(
    tableName = "travel_memory",
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tripId")]
)
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val place: String,
    val note: String,
    val photoUri: String?,
    val dateEpochMs: Long
)

@Entity(tableName = "sathi_chat")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUser: Boolean,
    val text: String,
    val timestampEpochMs: Long
)

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val lat: Double?,
    val lng: Double?,
    val note: String
)
