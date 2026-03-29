package com.merlos.powerdetector.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PowerActionDao {
    @Query("SELECT * FROM power_actions ORDER BY enabled DESC, updatedAt DESC")
    fun observeAll(): Flow<List<PowerActionEntity>>

    @Query("SELECT * FROM power_actions WHERE id = :actionId LIMIT 1")
    suspend fun findById(actionId: Long): PowerActionEntity?

    @Query("SELECT * FROM power_actions WHERE enabled = 1 AND trigger IN (:triggers) ORDER BY updatedAt DESC")
    suspend fun findEnabledForTriggers(triggers: List<String>): List<PowerActionEntity>

    @Upsert
    suspend fun upsert(action: PowerActionEntity): Long

    @Delete
    suspend fun delete(action: PowerActionEntity)

    @Query("UPDATE power_actions SET lastResult = :result, lastExecutedAt = :executedAt, updatedAt = :updatedAt WHERE id = :actionId")
    suspend fun updateExecution(actionId: Long, result: String, executedAt: Long, updatedAt: Long = System.currentTimeMillis())
}
