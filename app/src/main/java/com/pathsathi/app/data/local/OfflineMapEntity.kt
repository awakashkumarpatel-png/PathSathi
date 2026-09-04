package com.pathsathi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_maps")
data class OfflineMapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trekId: String,
    val trekName: String,
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val tileCount: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)
