package com.secretari.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.secretari.app.data.database.Converters
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date

@Entity(tableName = "audio_records")
@TypeConverters(Converters::class)
data class AudioRecord @OptIn(InternalSerializationApi::class) constructor(
    @PrimaryKey
    val recordDate: Long = Date().time,
    
    var transcript: String = "",
    
    var locale: RecognizerLocale = AppConstants.DEFAULT_SETTINGS.selectedLocale,
    
    var translatedLocale: RecognizerLocale? = null,
    
    var summary: Map<RecognizerLocale, String> = emptyMap(),
    
    var memo: List<MemoJsonData> = emptyList()
) {
    @InternalSerializationApi @kotlinx.serialization.Serializable
    data class MemoJsonData(
        val id: Int,
        val title: Map<RecognizerLocale, String>,
        val isChecked: Boolean
    )
    
    enum class TaskType {
        TRANSLATE, SUMMARIZE
    }
    
    @OptIn(InternalSerializationApi::class)
    fun resultFromAI(_taskType: TaskType, summaryText: String, settings: Settings) {
        when {
            settings.promptType == PromptType.SUMMARY || settings.promptType == PromptType.SUBSCRIPTION -> {
                val currentSummary = summary[locale] ?: ""
                summary = summary + (locale to "$currentSummary$summaryText\n")
            }
            settings.promptType == PromptType.CHECKLIST -> {
                // Parse JSON for checklist format
                try {
                    val jsonString = getAIJson(summaryText)
                    val json = Json.parseToJsonElement(jsonString)
                    if (json is JsonArray) {
                        val newMemoItems = mutableListOf<MemoJsonData>()
                        json.forEachIndexed { index, element ->
                            if (element is JsonObject) {
                                val id = element["id"]?.jsonPrimitive?.content?.toIntOrNull()
                                    ?: (index + 1)
                                val title = element["title"]?.jsonPrimitive?.content ?: "Unknown item"
                                val isChecked = element["isChecked"]?.jsonPrimitive?.content?.toBoolean() ?: false
                                
                                newMemoItems.add(
                                    MemoJsonData(
                                        id = id,
                                        title = mapOf(locale to title),
                                        isChecked = isChecked
                                    )
                                )
                            }
                        }
                        memo = newMemoItems
                    }
                } catch (e: Exception) {
                    println("Error parsing checklist JSON: ${e.message}")
                    // Fallback to summary format if JSON parsing fails
                    val currentSummary = summary[locale] ?: ""
                    summary = summary + (locale to "$currentSummary$summaryText\n")
                }
            }
        }
    }
    
    private fun getAIJson(aiJson: String): String {
        val regex = Regex("\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
        val str = aiJson.replace("\n", " ")
        val match = regex.find(str)
        return match?.let { "[${it.groupValues[1]}]" } ?: "[Invalid JSON data]"
    }
    
    companion object {
        @OptIn(InternalSerializationApi::class)
        fun sampleData(): List<AudioRecord> {
            return listOf(
                AudioRecord(
                    transcript = "Vodka is a clear distilled alcoholic beverage. Different varieties originated in Poland, Russia, and Sweden.",
                    summary = mapOf(RecognizerLocale.ENGLISH to "Vodka is a clear distilled alcoholic beverage.")
                )
            )
        }
    }
}

