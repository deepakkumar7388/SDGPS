package com.example.digitalpass.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches WHERE campus = :campus")
    fun getBatchesByCampus(campus: String): List<BatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(batches: List<BatchEntity>)

    @Query("DELETE FROM batches WHERE campus = :campus")
    fun deleteBatchesByCampus(campus: String)
}
