package com.example.digitalpass

import android.content.Context
import com.example.digitalpass.database.AppDatabase
import com.example.digitalpass.database.VisitorEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VisitorRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val visitorDao = db.visitorDao()
    private val apiService = RetrofitClient.instance

    suspend fun syncVisitors(token: String): List<VisitorEntity> {
        return withContext(Dispatchers.IO) {
            val lastSyncTime = LoginUserDataHolder.getLastVisitorSyncTime(context)
            var offset = 0
            val limit = 500
            var hasMore = true
            var latestServerTime = 0L

            while (hasMore) {
                val requestPayload = hashMapOf<String, Any>(
                    "token" to token,
                    "lastSyncTime" to lastSyncTime,
                    "offset" to offset,
                    "limit" to limit
                )

                val response = apiService.syncVisitorPasses(requestPayload).execute()
                if (response.isSuccessful) {
                    val syncData = response.body()
                    if (syncData != null) {
                        if (syncData.serverTime > 0L) {
                            latestServerTime = syncData.serverTime
                        }

                        if (syncData.updatedVisitors.isNotEmpty()) {
                            val entities = syncData.updatedVisitors.mapNotNull {
                                val visitorId = it["visitorId"]?.toDoubleOrNull()?.toInt() ?: it["visitorId"]?.toIntOrNull()
                                if (visitorId != null) VisitorEntity(visitorId, it) else null
                            }
                            visitorDao.insertAll(entities)
                        }

                        hasMore = syncData.hasMore
                        offset += limit
                    } else {
                        hasMore = false
                    }
                } else {
                    throw Exception(LoginUserDataHolder.getErrorMessage(response))
                }
            }

            if (latestServerTime > lastSyncTime) {
                LoginUserDataHolder.setLastVisitorSyncTime(context, latestServerTime)
            }

            visitorDao.getAllVisitors()
        }
    }

    suspend fun getActiveVisitors(todayStart: String, todayEnd: String): List<VisitorEntity> {
        return withContext(Dispatchers.IO) {
            visitorDao.getActiveVisitors(todayStart, todayEnd)
        }
    }

    suspend fun getHistoricalVisitors(todayStart: String): List<VisitorEntity> {
        return withContext(Dispatchers.IO) {
            visitorDao.getHistoricalVisitors(todayStart)
        }
    }

    suspend fun getVisitorsByDateRange(startDate: String, endDate: String): List<VisitorEntity> {
        return withContext(Dispatchers.IO) {
            visitorDao.getVisitorsByDateRange(startDate, endDate)
        }
    }
}
