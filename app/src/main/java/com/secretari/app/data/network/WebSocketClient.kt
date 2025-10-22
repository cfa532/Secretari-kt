package com.secretari.app.data.network

import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketClient(private val baseUrl: String = "wss://secretari.leither.uk") {
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    sealed class WebSocketMessage {
        data class StreamData(val data: String) : WebSocketMessage()
        data class Result(val answer: String, val cost: Double, val tokens: Long, val eof: Boolean) : WebSocketMessage()
        data class Error(val message: String) : WebSocketMessage()
    }
    
    fun connect(token: String, onMessage: (WebSocketMessage) -> Unit): Flow<WebSocketMessage> = callbackFlow {
        val url = "$baseUrl/secretari/ws/?token=$token"
        val request = Request.Builder().url(url).build()
        
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected to $url")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WebSocket", "Received message: $text")
                    val json = Json.parseToJsonElement(text).jsonObject
                    val type = json["type"]?.jsonPrimitive?.content
                    
                    val message = when (type) {
                        "stream" -> {
                            val data = json["data"]?.jsonPrimitive?.content ?: ""
                            WebSocketMessage.StreamData(data)
                        }
                        "result" -> {
                            val answer = json["answer"]?.jsonPrimitive?.content ?: ""
                            val cost = json["cost"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                            val tokens = json["tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                            val eof = json["eof"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                            WebSocketMessage.Result(answer, cost, tokens, eof)
                        }
                        "error" -> {
                            val errorMsg = json["message"]?.jsonPrimitive?.content ?: "Unknown error"
                            WebSocketMessage.Error(errorMsg)
                        }
                        else -> WebSocketMessage.Error("Unknown message type")
                    }
                    
                    trySend(message)
                    onMessage(message)
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message", e)
                    trySend(WebSocketMessage.Error(e.message ?: "Parse error"))
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection failed: ${t.message}", t)
                trySend(WebSocketMessage.Error(t.message ?: "Connection failed"))
                close()
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closing: $code $reason")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $code $reason")
                close()
            }
        }
        
        webSocket = client.newWebSocket(request, listener)
        
        awaitClose {
            Log.d("WebSocket", "Closing WebSocket connection")
            webSocket?.close(1000, "Client closed")
            webSocket = null
        }
    }
    
    fun sendMessage(
        rawText: String,
        prompt: String,
        defaultPrompt: String,
        promptType: String,
        hasPro: Boolean,
        llmParams: Map<String, String>
    ) {
        if (webSocket == null) {
            Log.e("WebSocket", "Cannot send message: WebSocket is null")
            return
        }
        
        val message = JSONObject().apply {
            put("input", JSONObject().apply {
                put("prompt", if (prompt.isEmpty()) defaultPrompt else prompt)
                put("prompt_type", promptType)
                put("rawtext", rawText)
                put("subscription", hasPro)
            })
            put("parameters", JSONObject().apply {
                put("llm", llmParams["llm"] ?: "openai")
                put("temperature", llmParams["temperature"] ?: "0.0")
            })
        }
        
        Log.d("WebSocket", "Sending message: $message")
        val success = webSocket?.send(message.toString()) ?: false
        if (!success) {
            Log.e("WebSocket", "Failed to send message - WebSocket may be closed")
        } else {
            Log.d("WebSocket", "Message sent successfully")
        }
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }
}

