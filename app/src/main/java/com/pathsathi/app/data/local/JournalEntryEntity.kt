package com.pathsathi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trekName: String,
    val title: String,
    val note: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rating: Int? = null,
    val timestamp: Long
)
