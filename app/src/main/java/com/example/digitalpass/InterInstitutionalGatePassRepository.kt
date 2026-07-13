package com.example.digitalpass

import android.content.Context
import com.example.digitalpass.database.GatePassEntity
import com.example.digitalpass.database.InterInstitutionalGatePassDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InterInstitutionalGatePassRepository(
    private val interInstitutionalGatePassDao: InterInstitutionalGatePassDao,
    private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("DigitalPassPrefs", Context.MODE_PRIVATE)

    suspend fun syncInterInstitutionalGatePasses(token: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            var offset = 0
            val limit = 500
            var hasMore = true

            val lastSyncTime = sharedPrefs.getLong("last_inter_institutional_gate_pass_sync_time", 0L)
            var newServerTime = lastSyncTime

            while (hasMore) {
                val payload = hashMapOf<String, Any>(
                    "token" to token,
                    "lastSyncTime" to lastSyncTime,
                    "offset" to offset,
                    "limit" to limit
                )

                val response = RetrofitClient.instance.syncInterInstitutionalGatePasses(payload).execute()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    
                    if (offset == 0 && body.deletedGatePassIds.isNotEmpty()) {
                        body.deletedGatePassIds.forEach { id ->
                            interInstitutionalGatePassDao.deleteByGatePassId(id)
                        }
                    }

                    if (body.updatedGatePasses.isNotEmpty()) {
                        val entities = body.updatedGatePasses.map { 
                            val gatePassId = it["gatePassId"]?.toDoubleOrNull()?.toInt() ?: 0
                            GatePassEntity(gatePassId = gatePassId, passData = it)
                        }
                        interInstitutionalGatePassDao.insertAll(entities)
                    }

                    newServerTime = body.serverTime
                    hasMore = body.hasMore
                    offset += limit
                } else {
                    throw Exception("Failed to sync inter-institutional gate passes")
                }
            }

            if (newServerTime > lastSyncTime) {
                sharedPrefs.edit().putLong("last_inter_institutional_gate_pass_sync_time", newServerTime).apply()
            }
            
            interInstitutionalGatePassDao.getAllGatePasses()
        }
    }

    suspend fun getActiveGatePasses(todayStart: String, todayEnd: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            val userRole = LoginUserDataHolder.loginUserData?.get("role") ?: ""
            val loginUserEmail = LoginUserDataHolder.loginUserData?.get("email") ?: ""

            if(userRole == "security guard")
                interInstitutionalGatePassDao.getActiveGatePassesBySecurity(todayStart, todayEnd)
            else 
                interInstitutionalGatePassDao.getActiveGatePassesByMember(todayStart, todayEnd, loginUserEmail, userRole)
        }
    }

    suspend fun getHistoricalGatePasses(todayStart: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            interInstitutionalGatePassDao.getHistoricalGatePasses(todayStart)
        }
    }

    suspend fun getGatePassesByDateRange(startDate: String, endDate: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            interInstitutionalGatePassDao.getGatePassesByDateRange(startDate, endDate)
        }
    }

    suspend fun getGatePassesByEmail(email: String): List<GatePassEntity> {
        return withContext(Dispatchers.IO) {
            interInstitutionalGatePassDao.getGatePassesByEmail(email)
        }
    }
}
