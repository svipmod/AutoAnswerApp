package com.autoanswer.ai

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiProviderImpl(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val model: String = "gpt-4o",
    private val systemPrompt: String = ""
) : AiProvider {

    override val name: String = "OpenAI ($model)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun answer(question: String, imageBase64: String?): String {
        val messages = mutableListOf<ChatMessage>()

        // System prompt
        if (systemPrompt.isNotBlank()) {
            messages.add(ChatMessage(role = "system", content = systemPrompt))
        }

        // User message - 支持图文
        val userContent = if (imageBase64 != null) {
            listOf(
                ContentPart(type = "text", text = question),
                ContentPart(
                    type = "image_url",
                    imageUrl = ImageUrl(url = "data:image/png;base64,$imageBase64")
                )
            )
        } else {
            question
        }
        messages.add(ChatMessage(role = "user", content = userContent))

        val request = ChatRequest(
            model = model,
            messages = messages,
            temperature = 0.3,
            max_tokens = 1024
        )

        val jsonBody = gson.toJson(request)
        val body = jsonBody.toRequestBody(jsonMediaType)

        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                chatResponse.choices?.firstOrNull()
                    ?.message?.content?.trim() ?: "无法解析AI响应"
            } else {
                val errorInfo = gson.fromJson(responseBody, ChatResponse::class.java)
                "API错误: ${errorInfo.error?.message ?: responseBody}"
            }
        } catch (e: IOException) {
            "网络错误: ${e.localizedMessage}"
        } catch (e: Exception) {
            "错误: ${e.localizedMessage}"
        }
    }
}
