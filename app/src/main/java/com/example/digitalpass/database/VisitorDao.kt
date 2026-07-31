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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVisitor(visitor: VisitorEntity)

    @Query("DELETE FROM visitors")
    fun deleteAllVisitors()

    @Query("SELECT * FROM visitors WHERE SUBSTR(json_extract(visitorData, '$.entryDate'), 1, 10) >= SUBSTR(:todayStart, 1, 10) AND SUBSTR(json_extract(visitorData, '$.entryDate'), 1, 10) <= SUBSTR(:todayEnd, 1, 10) AND json_extract(visitorData, '$.status') IN ('pending', 'meet') ORDER BY json_extract(visitorData, '$.entryDate') DESC")
    fun getActiveVisitors(todayStart: String, todayEnd: String): List<VisitorEntity>

    @Query("SELECT * FROM visitors WHERE SUBSTR(json_extract(visitorData, '$.entryDate'), 1, 10) < SUBSTR(:todayStart, 1, 10) OR json_extract(visitorData, '$.status') NOT IN ('pending', 'meet') ORDER BY json_extract(visitorData, '$.entryDate') DESC")
    fun getHistoricalVisitors(todayStart: String): List<VisitorEntity>

    @Query("SELECT * FROM visitors WHERE SUBSTR(json_extract(visitorData, '$.entryDate'), 1, 10) >= SUBSTR(:startDate, 1, 10) AND SUBSTR(json_extract(visitorData, '$.entryDate'), 1, 10) <= SUBSTR(:endDate, 1, 10) ORDER BY json_extract(visitorData, '$.entryDate') DESC")
    fun getVisitorsByDateRange(startDate: String, endDate: String): List<VisitorEntity>
}
