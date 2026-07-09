package com.example.digitalpass

data class SyncUserResponse(
    val updatedUsers: ArrayList<HashMap<String, String>>,
    val deletedEmails: ArrayList<String>,
    val hasMore: Boolean = false,
    val serverTime: Long = 0L
)
