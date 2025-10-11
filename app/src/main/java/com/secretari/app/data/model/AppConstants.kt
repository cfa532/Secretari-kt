package com.secretari.app.data.model

import java.util.Locale

object AppConstants {
    const val MAX_SILENT_SECONDS = 1800  // 30 minutes
    const val MAX_RECORD_SECONDS = 28800  // 8 hours
    const val NUM_RECORDS_IN_DB = 30
    const val RECORDER_TIMER_FREQUENCY = 10.0  // seconds
    
    val PRIMARY_MODEL = LLMModel.GPT_4O
    const val SIGNUP_BONUS = 0.2
    const val DEFAULT_PASSWORD = "zaq12WSX"
    
    private val defaultPrompts = mapOf(
        PromptType.SUMMARY to mapOf(
            RecognizerLocale.ENGLISH to "You are an intelligent secretary. Extract the important content from the following text and make a comprehensive summary. Divide it into appropriate sections. The output format should be plain text. ",
            RecognizerLocale.CHINESE to "你是個智能秘書。 提取下述文字中的重要內容，做一份全面的摘要。并适当分段。输出格式用纯文本。",
            RecognizerLocale.JAPANESE to "あなたはインテリジェントな秘書です。以下のテキストから重要な内容を抽出し、包括的な要約を作成してください。適切に段落を分けてください。出力形式はプレーンテキストでお願いします。",
            RecognizerLocale.SPANISH to "Eres un secretario inteligente. Extrae el contenido importante del siguiente texto y haz un resumen completo. Divide el texto en secciones apropiadas. El formato de salida debe ser texto plano. ",
            RecognizerLocale.INDONESIAN to "Anda adalah sekretaris cerdas. Ekstrak konten penting dari teks berikut dan buat ringkasan yang komprehensif. Silakan bagi menjadi beberapa bagian yang sesuai. Format keluaran harus dalam teks biasa. ",
            RecognizerLocale.KOREAN to "당신은 똑똑한 비서입니다. 다음 텍스트에서 중요한 내용을 추출하여 포괄적인 요약을 작성하세요. 적절한 섹션으로 나누세요. ",
            RecognizerLocale.VIETNAMESE to "Bạn là một thư ký thông minh. Trích xuất nội dung quan trọng từ văn bản sau và tạo thành một bản tóm tắt toàn diện. Chia nó thành các phần thích hợp. Định dạng đầu ra phải là văn bản thuần túy. ",
            RecognizerLocale.THAI to "คุณเป็นเลขาที่ชาญฉลาด แยกเนื้อหาที่สำคัญออกจากข้อความต่อไปนี้และสรุปให้ครอบคลุม แบ่งออกเป็นส่วนๆ ตามความเหมาะสม รูปแบบผลลัพธ์ควรเป็นข้อความธรรมดา "
        ),
        PromptType.SUBSCRIPTION to mapOf(
            RecognizerLocale.ENGLISH to "The following text is a voice recording. Organize the text, remove meaningless clichés, repeated content, etc., and divide it into appropriate paragraphs. ",
            RecognizerLocale.CHINESE to "下述文字是一段語音錄音。整理該段文字，去除無意義的口頭禪、重複內容等，並適當分段。 ",
            RecognizerLocale.JAPANESE to "以下のテキストは音声録音です。テキストを整理し、意味のない決まり文句や繰り返しの内容などを削除し、適切な段落に分割します。 ",
            RecognizerLocale.SPANISH to "El siguiente texto es una grabación de voz. Organiza el texto, elimina clichés sin sentido, contenido repetido, etc. y divídelo en párrafos apropiados. ",
            RecognizerLocale.INDONESIAN to "Teks berikut adalah rekaman suara. Atur teks, hilangkan klise yang tidak berarti, konten yang berulang, dsb., dan bagi ke dalam paragraf yang sesuai. ",
            RecognizerLocale.KOREAN to "다음 텍스트는 음성 녹음입니다. 텍스트를 구성하고, 무의미한 상투적인 표현, 반복되는 내용 등을 제거한 후 적절한 문단으로 나눕니다. ",
            RecognizerLocale.VIETNAMESE to "Đoạn văn sau đây là bản ghi âm giọng nói. Sắp xếp văn bản, loại bỏ những câu sáo rỗng vô nghĩa, nội dung lặp lại, v.v. và chia thành các đoạn văn phù hợp. ",
            RecognizerLocale.THAI to "ข้อความต่อไปนี้เป็นการบันทึกเสียง จัดระเบียบข้อความ ลบคำซ้ำที่ไม่มีความหมาย เนื้อหาที่ซ้ำกัน ฯลฯ และแบ่งออกเป็นย่อหน้าที่เหมาะสม "
        ),
        PromptType.CHECKLIST to mapOf(
            RecognizerLocale.ENGLISH to """
            You are a smart assistant. Extract the important content from the rawtext below and make a comprehensive memo. The output format uses the following JSON sequence, where title is the item content of the memo.
             [
               {
                 "id": 1,
                 "title": "Item 1",
                 "isChecked": false
               },
               {
                 "id": 2,
                 "title": "Item 2",
                 "isChecked": false
               },
               {
                 "id": 3,
                 "title": "Item 3",
                 "isChecked": false
               }
             ]
             
             rawtext:
             
           """.trimIndent(),
            RecognizerLocale.CHINESE to """
            你是個智能秘書。 提取下述 rawtext 中的重要內容，做一份全面的备忘录。輸出格式採用下述 JSON 序列。其中 title 是備忘錄的條目內容。
            [
              {
                "id": 1,
                "title": "Item 1",
                "isChecked": false
              },
              {
                "id": 2,
                "title": "Item 2",
                "isChecked": false
              },
              {
                "id": 3,
                "title": "Item 3",
                "isChecked": false
              }
            ]
            
            rawtext:
            
            """.trimIndent()
        )
    )
    
    val DEFAULT_SETTINGS = Settings(
        prompt = defaultPrompts,
        serverURL = "bunny.leither.uk/secretari",
        audioSilentDB = "-40",
        selectedLocale = systemLanguage(),
        promptType = PromptType.SUMMARY,
        llmParams = mapOf("llm" to "openai", "temperature" to "0.0")
    )
    
    private fun systemLanguage(): RecognizerLocale {
        val languageCode = Locale.getDefault().language
        return RecognizerLocale.fromCode(languageCode)
    }
}

