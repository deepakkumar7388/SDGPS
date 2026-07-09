package com.example.digitalpass

data class SyncGatePassResponse(
    val updatedGatePasses: ArrayList<HashMap<String, String>>,
    val deletedGatePassIds: ArrayList<Int>,
    val hasMore: Boolean = false,
    val serverTime: Long = 0L
)
