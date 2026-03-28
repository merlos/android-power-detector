package com.merlos.powerdetector.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "power_actions")
data class PowerActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val trigger: String,
    val recipient: String,
    val botToken: String? = null,
    val message: String,
    val enabled: Boolean = true,
    val lastResult: String? = null,
    val lastExecutedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable
