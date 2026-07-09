package com.example.digitalpass.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "gate_passes")
data class GatePassEntity(
    @PrimaryKey val gatePassId: Int,
    val passData: HashMap<String, String>
) : Serializable
