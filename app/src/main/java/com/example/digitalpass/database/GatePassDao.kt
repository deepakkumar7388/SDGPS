package com.example.digitalpass.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GatePassDao {
    @Query("SELECT * FROM gate_passes WHERE json_extract(passData, '$.destinationCampus') IS NULL")
    fun getAllGatePasses(): List<GatePassEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(passes: List<GatePassEntity>)

    @Query("DELETE FROM gate_passes WHERE gatePassId = :gatePassId")
    fun deleteByGatePassId(gatePassId: Int)

    @Query("DELETE FROM gate_passes WHERE json_extract(passData, '$.destinationCampus') IS NULL")
    fun deleteAllGatePasses()

    @Query("SELECT * FROM gate_passes WHERE json_extract(passData, '$.destinationCampus') IS NULL AND json_extract(passData, '$.applyDate') >= :todayStart AND json_extract(passData, '$.applyDate') <= :todayEnd AND json_extract(passData, '$.status') IN ('pending', 'approving', 'approved') AND (json_extract(passData, '$.applyEmail') != :loginUserEmail OR :userRole IN ('principal', 'hod')) ORDER BY json_extract(passData, '$.applyDate') DESC")
    fun getActiveGatePassesByMember(todayStart: String, todayEnd: String, loginUserEmail: String, userRole: String): List<GatePassEntity>

    @Query("SELECT * FROM gate_passes WHERE json_extract(passData, '$.destinationCampus') IS NULL AND json_extract(passData, '$.applyDate') >= :todayStart AND json_extract(passData, '$.applyDate') <= :todayEnd AND json_extract(passData, '$.status') IN ('approved') ORDER BY json_extract(passData, '$.applyDate') DESC")
    fun getActiveGatePassesBySecurity(todayStart: String, todayEnd: String): List<GatePassEntity>

    @Query("SELECT * FROM gate_passes WHERE json_extract(passData, '$.destinationCampus') IS NULL AND (json_extract(passData, '$.applyDate') < :todayStart OR json_extract(passData, '$.status') NOT IN ('pending', 'approving', 'approved')) ORDER BY json_extract(passData, '$.applyDate') DESC")
    fun getHistoricalGatePasses(todayStart: String): List<GatePassEntity>

    @Query("SELECT * FROM gate_passes WHERE json_extract(passData, '$.destinationCampus') IS NULL AND json_extract(passData, '$.applyDate') >= :startDate AND json_extract(passData, '$.applyDate') <= :endDate ORDER BY json_extract(passData, '$.applyDate') DESC")
    fun getGatePassesByDateRange(startDate: String, endDate: String): List<GatePassEntity>

    @Query("SELECT * FROM gate_passes WHERE json_extract(passData, '$.destinationCampus') IS NULL AND json_extract(passData, '$.applyEmail') = :email ORDER BY json_extract(passData, '$.applyDate') DESC")
    fun getGatePassesByEmail(email: String): List<GatePassEntity>
    @Query("SELECT * FROM gate_passes WHERE json_extract(passData, '$.applyEmail') = :email ORDER BY json_extract(passData, '$.applyDate') DESC")
    fun getAllGatePassesByEmail(email: String): List<GatePassEntity>
}
