package com.example.digitalpass.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CampusDao {
    @Query("SELECT * FROM campuses")
    suspend fun getAllCampuses(): List<CampusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampuses(campuses: List<CampusEntity>)

    @Query("SELECT COUNT(*) FROM campuses")
    suspend fun getCampusCount(): Int
}
