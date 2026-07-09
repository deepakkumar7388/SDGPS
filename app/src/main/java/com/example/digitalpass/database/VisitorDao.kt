package com.example.digitalpass.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VisitorDao {
    @Query("SELECT * FROM visitors")
    fun getAllVisitors(): List<VisitorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(visitors: List<VisitorEntity>)

    @Query("DELETE FROM visitors")
    fun deleteAllVisitors()

    @Query("SELECT * FROM visitors WHERE json_extract(visitorData, '$.entryDate') >= :todayStart AND json_extract(visitorData, '$.entryDate') <= :todayEnd AND json_extract(visitorData, '$.status') IN ('pending', 'meet') ORDER BY json_extract(visitorData, '$.entryDate') DESC")
    fun getActiveVisitors(todayStart: String, todayEnd: String): List<VisitorEntity>

    @Query("SELECT * FROM visitors WHERE json_extract(visitorData, '$.entryDate') < :todayStart OR json_extract(visitorData, '$.status') NOT IN ('pending', 'meet') ORDER BY json_extract(visitorData, '$.entryDate') DESC")
    fun getHistoricalVisitors(todayStart: String): List<VisitorEntity>

    @Query("SELECT * FROM visitors WHERE json_extract(visitorData, '$.entryDate') >= :startDate AND json_extract(visitorData, '$.entryDate') <= :endDate ORDER BY json_extract(visitorData, '$.entryDate') DESC")
    fun getVisitorsByDateRange(startDate: String, endDate: String): List<VisitorEntity>
}
