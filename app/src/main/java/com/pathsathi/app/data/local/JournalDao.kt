package com.pathsathi.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<JournalEntryEntity>>

    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity)

    @Delete
    suspend fun delete(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<JournalEntryEntity>>
}
