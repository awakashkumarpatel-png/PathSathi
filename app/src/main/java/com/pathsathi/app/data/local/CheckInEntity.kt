package com.pathsathi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** status is one of: "confirmed", "missed" */
@Entity(tableName = "checkins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledAt: Long,
    val respondedAt: Long? = null,
    val status: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryPercent: Int? = null
)
