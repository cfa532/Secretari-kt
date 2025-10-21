package com.secretari.app.data.model

enum class RecognizerLocale(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    CHINESE("zh", "中文"),
    SPANISH("es", "Español"),
    INDONESIAN("id", "Indonesia"),
    KOREAN("ko", "한국인"),
    FILIPINO("phi", "Filipino"),
    VIETNAMESE("vi", "ViệtNam"),
    THAI("th", "แบบไทย");

    companion object {
        fun fromCode(code: String): RecognizerLocale {
            return RecognizerLocale.entries.find { it.code == code } ?: ENGLISH
        }

        fun getAvailable(): List<RecognizerLocale> {
            return listOf(ENGLISH, JAPANESE, CHINESE, KOREAN, SPANISH, INDONESIAN, VIETNAMESE, THAI)
        }
    }
}

