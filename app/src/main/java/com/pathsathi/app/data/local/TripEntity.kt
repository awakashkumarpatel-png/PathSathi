package com.pathsathi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pathsathi.app.data.model.Trip

private const val LIST_DELIMITER = "||"

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripName: String,
    val destination: String,
    val startDateTime: Long,
    val endDateTime: Long,
    val travelWith: String,
    val membersCsv: String,
    val stopsCsv: String,
    val stay: String,
    val budget: Double?,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)

fun TripEntity.toTrip(): Trip = Trip(
    id = id,
    tripName = tripName,
    destination = destination,
    startDateTime = startDateTime,
    endDateTime = endDateTime,
    travelWith = travelWith,
    members = if (membersCsv.isBlank()) emptyList() else membersCsv.split(LIST_DELIMITER),
    stops = if (stopsCsv.isBlank()) emptyList() else stopsCsv.split(LIST_DELIMITER),
    stay = stay,
    budget = budget,
    notes = notes,
    createdAt = createdAt
)

fun Trip.toEntity(): TripEntity = TripEntity(
    id = id,
    tripName = tripName,
    destination = destination,
    startDateTime = startDateTime,
    endDateTime = endDateTime,
    travelWith = travelWith,
    membersCsv = members.joinToString(LIST_DELIMITER),
    stopsCsv = stops.joinToString(LIST_DELIMITER),
    stay = stay,
    budget = budget,
    notes = notes,
    createdAt = createdAt
)
