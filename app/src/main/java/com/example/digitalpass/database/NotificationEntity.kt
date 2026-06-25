package com.example.digitalpass.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val notificationId:Int,
    val title: String,
    val body: String,
    val name: String,
    val imgUrl: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
