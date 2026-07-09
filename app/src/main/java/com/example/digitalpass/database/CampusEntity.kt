package com.example.digitalpass.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campuses")
data class CampusEntity(
    @PrimaryKey
    val name: String
)
