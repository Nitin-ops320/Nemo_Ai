package com.nemo.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── PASTE YOUR KEY HERE ──────────────────────────────────
    private val apiKey = "AIzaSyAdIvxgLTuwycbZgAE66gV0fsuxA465CDk"
    // ─────────────────────────────────────────────────────────

    private val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

    suspend fun think(userCommand: String, screenContent: String): GeminiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(userCommand, screenContent)
                val body = buildRequestBody(prompt)
                val request = Request.Builder()
                    .url(url)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""
                parseResponse(responseText)

            } catch (e: Exception) {
                GeminiResponse(
                    action = "error",
                    message = "Error: ${e.message}",
                    target = null
                )
            }
        }
    }

    private fun buildPrompt(command: String, screen: String): String {
        return """
            You are Nemo, an AI assistant that controls an Android phone.
            
            Current screen content:
            $screen
            
            User command: $command
            
            Respond ONLY in this exact JSON format:
            {
                "action": "tap" or "scroll" or "open_app" or "read" or "reply",
                "message": "what you are doing or found",
                "target": "text to tap or app name or null"
            }
            
            Rules:
            - If user wants to open an app, use action "open_app" and put app name in target
            - If user wants to tap something, use action "tap" and put the text of element in target
            - If user wants to know what is on screen, use action "read" and summarize screen in message
            - Keep message short and clear
            - Always respond in valid JSON only, nothing else
        """.trimIndent()
    }

    private fun buildRequestBody(prompt: String): String {
        val content = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", prompt)
                })
            })
        }
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(content)
            })
        }.toString()
    }

    private fun parseResponse(raw: String): GeminiResponse {
        return try {
            val json = JSONObject(raw)
            val text = json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removePrefix("```json")
                .removeSuffix("```")
                .trim()

            val result = JSONObject(text)
            GeminiResponse(
                action = result.optString("action", "read"),
                message = result.optString("message", "Done"),
                target = result.optString("target").takeIf { it != "null" }
            )
        } catch (e: Exception) {
            GeminiResponse(
                action = "error",
                message = "Could not understand response",
                target = null
            )
        }
    }
}

data class GeminiResponse(
    val action: String,
    val message: String,
    val target: String?
)
