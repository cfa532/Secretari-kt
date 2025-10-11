package com.secretari.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.secretari.app.data.database.Converters
import java.util.Date

@Entity(tableName = "audio_records")
@TypeConverters(Converters::class)
data class AudioRecord(
    @PrimaryKey
    val recordDate: Long = Date().time,
    
    var transcript: String = "",
    
    var locale: RecognizerLocale = AppConstants.DEFAULT_SETTINGS.selectedLocale,
    
    var translatedLocale: RecognizerLocale? = null,
    
    var summary: Map<RecognizerLocale, String> = emptyMap(),
    
    var memo: List<MemoJsonData> = emptyList()
) {
    @kotlinx.serialization.Serializable
    data class MemoJsonData(
        val id: Int,
        val title: Map<RecognizerLocale, String>,
        val isChecked: Boolean
    )
    
    enum class TaskType {
        TRANSLATE, SUMMARIZE
    }
    
    fun resultFromAI(taskType: TaskType, summaryText: String, settings: Settings) {
        when {
            settings.promptType == PromptType.SUMMARY || settings.promptType == PromptType.SUBSCRIPTION -> {
                val currentSummary = summary[locale] ?: ""
                summary = summary + (locale to "$currentSummary$summaryText\n")
            }
            else -> {
                // Memo/checklist type - parse JSON
                try {
                    val jsonString = getAIJson(summaryText)
                    // Parse JSON and update memo
                    // This would need proper JSON parsing in real implementation
                } catch (e: Exception) {
                    println("Error parsing JSON: ${e.message}")
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

