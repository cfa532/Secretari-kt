package com.secretari.app.data.repository

import com.secretari.app.data.database.AudioRecordDao
import com.secretari.app.data.model.AppConstants
import com.secretari.app.data.model.AudioRecord
import kotlinx.coroutines.flow.Flow

class AudioRecordRepository(private val audioRecordDao: AudioRecordDao) {
    
    val allRecords: Flow<List<AudioRecord>> = audioRecordDao.getAllRecords()
    
    suspend fun getRecordByDate(recordDate: Long): AudioRecord? {
        return audioRecordDao.getRecordByDate(recordDate)
    }
    
    suspend fun insert(record: AudioRecord) {
        audioRecordDao.insert(record)
        
        // Maintain max number of records
        val count = audioRecordDao.getRecordCount()
        if (count > AppConstants.NUM_RECORDS_IN_DB) {
            audioRecordDao.deleteOldRecords(AppConstants.NUM_RECORDS_IN_DB)
        }
    }
    
    suspend fun update(record: AudioRecord) {
        audioRecordDao.update(record)
    }
    
    suspend fun delete(record: AudioRecord) {
        audioRecordDao.delete(record)
    }
    
    suspend fun deleteAll() {
        audioRecordDao.deleteAll()
    }
}

