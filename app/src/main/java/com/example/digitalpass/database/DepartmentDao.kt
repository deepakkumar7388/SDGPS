package com.example.digitalpass.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments WHERE type = :type")
    suspend fun getDepartmentsByType(type: String): List<DepartmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartments(departments: List<DepartmentEntity>)

    @Query("SELECT COUNT(*) FROM departments WHERE type = :type")
    suspend fun getDepartmentCount(type: String): Int
}
