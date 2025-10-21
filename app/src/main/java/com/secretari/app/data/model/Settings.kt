package com.secretari.app.data.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi @Serializable
data class Settings(
    val prompt: Map<PromptType, Map<RecognizerLocale, String>> = emptyMap(),
    val serverURL: String = "bunny.leither.uk/secretari",
    val audioSilentDB: String = "-40",
    val selectedLocale: RecognizerLocale = RecognizerLocale.ENGLISH,
    val promptType: PromptType = PromptType.SUMMARY,
    val llmParams: Map<String, String> = mapOf("llm" to "openai", "temperature" to "0.0")
)

enum class PromptType(val value: String) {
    SUMMARY("summary"),
    CHECKLIST("checklist"),
    SUBSCRIPTION("subscript");

    companion object {
        fun allowedCases(lowBalance: Boolean): List<PromptType> {
            return if (lowBalance) listOf(SUMMARY) else PromptType.entries
        }
    }
}

enum class LLM(val value: String) {
    OPENAI("openai"),
    GEMINI("gemini")
}

enum class LLMModel(val value: String) {
    GPT_3("gpt-3.5-turbo"),
    GPT_4("gpt-4"),
    GPT_4_TURBO("gpt-4-turbo"),
    GPT_4O("gpt-4o")
}

