package com.example.digitalpass

data class SyncVisitorResponse(
    val updatedVisitors: ArrayList<HashMap<String, String>>,
    val hasMore: Boolean = false,
    val serverTime: Long = 0L
)
