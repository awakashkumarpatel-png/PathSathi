package com.pathsathi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_plans")
data class TripPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trekId: String,
    val trekName: String,
    val startingPoint: String,
    val startDate: Long,
    val endDate: Long,
    val travelers: Int,
    val budget: Double,
    val itineraryJson: String,
    val estimatedCost: Double,
    val createdAt: Long = System.currentTimeMillis()
)
