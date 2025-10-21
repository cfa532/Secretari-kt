package com.secretari.app.data.database

import androidx.room.TypeConverter
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.RecognizerLocale
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    
    @TypeConverter
    fun fromRecognizerLocale(value: RecognizerLocale): String {
        return value.code
    }
    
    @TypeConverter
    fun toRecognizerLocale(value: String): RecognizerLocale {
        return RecognizerLocale.fromCode(value)
    }
    
    @TypeConverter
    fun fromRecognizerLocaleNullable(value: RecognizerLocale?): String? {
        return value?.code
    }
    
    @TypeConverter
    fun toRecognizerLocaleNullable(value: String?): RecognizerLocale? {
        return value?.let { RecognizerLocale.fromCode(it) }
    }
    
    @TypeConverter
    fun fromSummaryMap(value: Map<RecognizerLocale, String>): String {
        val stringMap = value.mapKeys { it.key.code }
        return Json.encodeToString(stringMap)
    }
    
    @TypeConverter
    fun toSummaryMap(value: String): Map<RecognizerLocale, String> {
        if (value.isEmpty()) return emptyMap()
        return try {
            val stringMap: Map<String, String> = Json.decodeFromString(value)
            stringMap.mapKeys { RecognizerLocale.fromCode(it.key) }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    @OptIn(InternalSerializationApi::class)
    @TypeConverter
    fun fromMemoList(value: List<AudioRecord.MemoJsonData>): String {
        return Json.encodeToString(value)
    }
    
    @OptIn(InternalSerializationApi::class)
    @TypeConverter
    fun toMemoList(value: String): List<AudioRecord.MemoJsonData> {
        if (value.isEmpty()) return emptyList()
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

