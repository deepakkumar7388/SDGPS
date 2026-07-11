package com.example.digitalpass

import android.content.Context
import com.example.digitalpass.database.AppDatabase
import com.example.digitalpass.database.GatePassEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GatePassRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val gatePassDao = db.gatePassDao()
    private val apiService = RetrofitClient.instance

    suspend fun syncGatePasses(token: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            val lastSyncTime = LoginUserDataHolder.getLastGatePassSyncTime(context)
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

                val response = apiService.syncGatePasses(requestPayload).execute()
                if (response.isSuccessful) {
                    val syncData = response.body()
                    if (syncData != null) {
                        if (syncData.serverTime > 0L) {
                            latestServerTime = syncData.serverTime
                        }

                        if (syncData.updatedGatePasses.isNotEmpty()) {
                            val entities = syncData.updatedGatePasses.mapNotNull {
                                val gatePassId = it["gatePassId"]?.toDoubleOrNull()?.toInt() ?: it["gatePassId"]?.toIntOrNull()
                                if (gatePassId != null) GatePassEntity(gatePassId, it) else null
                            }
                            gatePassDao.insertAll(entities)
                        }

                        if (syncData.deletedGatePassIds.isNotEmpty()) {
                            syncData.deletedGatePassIds.forEach { id ->
                                gatePassDao.deleteByGatePassId(id)
                            }
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
                LoginUserDataHolder.setLastGatePassSyncTime(context, latestServerTime)
            }

            gatePassDao.getAllGatePasses()
        }
    }

    suspend fun getActiveGatePasses(todayStart: String, todayEnd: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            val userRole = LoginUserDataHolder.loginUserData?.get("role") ?: ""
            val loginUserEmail = LoginUserDataHolder.loginUserData?.get("email") ?: ""

            if(userRole == "security guard")
                gatePassDao.getActiveGatePassesBySecurity(todayStart, todayEnd)
            else 
                gatePassDao.getActiveGatePassesByMember(todayStart, todayEnd, loginUserEmail, userRole)
        }
    }

    suspend fun getHistoricalGatePasses(todayStart: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            gatePassDao.getHistoricalGatePasses(todayStart)
        }
    }

    suspend fun getGatePassesByDateRange(startDate: String, endDate: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            gatePassDao.getGatePassesByDateRange(startDate, endDate)
        }
    }

    suspend fun getGatePassesByEmail(email: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            gatePassDao.getGatePassesByEmail(email)
        }
    }
}
