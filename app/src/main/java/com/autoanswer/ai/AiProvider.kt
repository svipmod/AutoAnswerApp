package com.autoanswer.ai

import com.google.gson.annotations.SerializedName

/** AI提供商接口 */
interface AiProvider {
    val name: String
    suspend fun answer(question: String, imageBase64: String? = null): String
}

/** 支持的AI类型 */
enum class AiType(val displayName: String, val defaultUrl: String) {
    OPEN_AI("OpenAI", "https://api.openai.com/v1"),
    AZURE("Azure OpenAI", ""),
    CLAUDE("Claude", "https://api.anthropic.com/v1"),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1"),
    QWEN("通义千问", "https://dashscope.aliyuncs.com/api/v1"),
    BAIDU("百度文心", "https://aip.baidubce.com/rpc/2.0/ai_custom"),
    MOONSHOT("Moonshot", "https://api.moonshot.cn/v1"),
    ZHIPU("智谱清言", "https://open.bigmodel.cn/api/paas/v4"),
    CUSTOM("自定义", "")
}

/** API请求统一格式 */
data class ChatRequest(
    val model: String = "gpt-4o",
    val messages: List<ChatMessage> = emptyList(),
    val temperature: Double = 0.3,
    val max_tokens: Int = 1024
)

data class ChatMessage(
    val role: String = "user",
    val content: Any = "" // String 或 List<ContentPart>
)

data class ContentPart(
    val type: String = "text",
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    val url: String = ""
)

/** API响应统一格式 */
data class ChatResponse(
    val choices: List<Choice>? = null,
    val error: ErrorInfo? = null
)

data class Choice(
    val message: ChoiceMessage? = null
)

data class ChoiceMessage(
    val content: String? = null
)

data class ErrorInfo(
    val message: String? = null
)
