package com.secretari.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.secretari.app.data.model.AudioRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioRecordDao {
    
    @Query("SELECT * FROM audio_records ORDER BY recordDate DESC")
    fun getAllRecords(): Flow<List<AudioRecord>>
    
    @Query("SELECT * FROM audio_records WHERE recordDate = :recordDate")
    suspend fun getRecordByDate(recordDate: Long): AudioRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AudioRecord)
    
    @Update
    suspend fun update(record: AudioRecord)
    
    @Delete
    suspend fun delete(record: AudioRecord)
    
    @Query("DELETE FROM audio_records")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM audio_records")
    suspend fun getRecordCount(): Int
    
    @Query("DELETE FROM audio_records WHERE recordDate NOT IN (SELECT recordDate FROM audio_records ORDER BY recordDate DESC LIMIT :limit)")
    suspend fun deleteOldRecords(limit: Int)
}

