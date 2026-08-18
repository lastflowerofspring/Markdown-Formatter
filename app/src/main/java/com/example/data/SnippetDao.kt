package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {
    @Query("SELECT * FROM formatted_snippets ORDER BY createdAt DESC")
    fun getAllSnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM formatted_snippets WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteSnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM formatted_snippets WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchSnippets(query: String): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM formatted_snippets WHERE id = :id")
    suspend fun getSnippetById(id: Long): SnippetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity): Long

    @Update
    suspend fun updateSnippet(snippet: SnippetEntity)

    @Delete
    suspend fun deleteSnippet(snippet: SnippetEntity)

    @Query("DELETE FROM formatted_snippets")
    suspend fun clearAll()
}
