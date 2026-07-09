package com.example.digitalpass

import android.content.Context
import com.example.digitalpass.database.CampusDao
import com.example.digitalpass.database.CampusEntity
import com.example.digitalpass.database.DepartmentDao
import com.example.digitalpass.database.DepartmentEntity
import com.example.digitalpass.database.UserDao
import com.example.digitalpass.database.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserOperationRepository(
    private val context: Context,
    private val campusDao: CampusDao,
    private val departmentDao: DepartmentDao,
    private val userDao: UserDao,
    private val apiService: ApiService
) {
    suspend fun getCampuses(token: String): List<String> {
        return withContext(Dispatchers.IO) {
            val count = campusDao.getCampusCount()
            if (count > 0) {
                // Fetch from Room Database
                campusDao.getAllCampuses().map { it.name }
            } else {
                // Fetch from Network
                val response = apiService.getCampusForAllotment(token).execute()
                if (response.isSuccessful) {
                    val campuses = response.body() ?: arrayListOf()
                    val entities = campuses.map { CampusEntity(it) }
                    campusDao.insertCampuses(entities)
                    campuses
                } else {
                    throw Exception(LoginUserDataHolder.getErrorMessage(response))
                }
            }
        }
    }

    suspend fun getDepartments(token: String, type: String): List<String> {
        return withContext(Dispatchers.IO) {
            val count = departmentDao.getDepartmentCount(type)
            if (count > 0) {
                // Fetch from Room Database
                departmentDao.getDepartmentsByType(type).map { it.name }
            } else {
                // Fetch from Network
                val hashForDepartment = hashMapOf("token" to token, "type" to type)
                val response = apiService.getAllDepartment(hashForDepartment).execute()
                if (response.isSuccessful) {
                    val departments = response.body() ?: arrayListOf()
                    val entities = departments.map { DepartmentEntity(name = it, type = type) }
                    departmentDao.insertDepartments(entities)
                    departments
                } else {
                    throw Exception(LoginUserDataHolder.getErrorMessage(response))
                }
            }
        }
    }

    suspend fun syncUsers(token: String): List<UserEntity> {
        return withContext(Dispatchers.IO) {
            val lastSyncTime = LoginUserDataHolder.getLastUserSyncTime(context)
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
                
                val response = apiService.getMembersForUserManagement(requestPayload).execute()
                if (response.isSuccessful) {
                    val syncData = response.body()
                    if (syncData != null) {
                        // Capture server time from the response
                        if (syncData.serverTime > 0L) {
                            latestServerTime = syncData.serverTime
                        }

                        // Update or insert changed users
                        if (syncData.updatedUsers.isNotEmpty()) {
                            val entities = syncData.updatedUsers.mapNotNull {
                                val email = it["email"]
                                if (!email.isNullOrEmpty()) UserEntity(email, it) else null
                            }
                            userDao.insertAll(entities)
                        }
                        
                        // Remove deleted users (only comes in first chunk usually, but safe to loop)
                        if (syncData.deletedEmails.isNotEmpty()) {
                            syncData.deletedEmails.forEach { email ->
                                userDao.deleteUserByEmail(email)
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
            
            // After all pagination loops finish, save the authoritative server time
            if (latestServerTime > lastSyncTime) {
                LoginUserDataHolder.setLastUserSyncTime(context, latestServerTime)
            }
            
            // Return all locally cached users to display
            userDao.getAllUsers()
        }
    }
}
