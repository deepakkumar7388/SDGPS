package com.example.digitalpass.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "visitors")
data class VisitorEntity(
    @PrimaryKey val visitorId: Int,
    val visitorData: HashMap<String, String>
) : Serializable
