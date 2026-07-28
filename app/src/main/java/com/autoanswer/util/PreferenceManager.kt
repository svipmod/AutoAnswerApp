package com.autoanswer.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auto_answer_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // AI 配置
    var aiProvider: String
        get() = prefs.getString("ai_provider", "openai") ?: "openai"
        set(value) = prefs.edit().putString("ai_provider", value).apply()

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var apiUrl: String
        get() = prefs.getString("api_url", "") ?: ""
        set(value) = prefs.edit().putString("api_url", value).apply()

    var modelName: String
        get() = prefs.getString("model_name", "gpt-4o") ?: "gpt-4o"
        set(value) = prefs.edit().putString("model_name", value).apply()

    // 答题配置
    var autoAnswerEnabled: Boolean
        get() = prefs.getBoolean("auto_answer_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_answer_enabled", value).apply()

    var answerDelay: Int
        get() = prefs.getInt("answer_delay", 2000)
        set(value) = prefs.edit().putInt("answer_delay", value).apply()

    var captureInterval: Int
        get() = prefs.getInt("capture_interval", 3000)
        set(value) = prefs.edit().putInt("capture_interval", value).apply()

    // 安全配置
    var antiSwitchEnabled: Boolean
        get() = prefs.getBoolean("anti_switch_enabled", true)
        set(value) = prefs.edit().putBoolean("anti_switch_enabled", value).apply()

    var antiRecordEnabled: Boolean
        get() = prefs.getBoolean("anti_record_enabled", true)
        set(value) = prefs.edit().putBoolean("anti_record_enabled", value).apply()

    // 自定义Prompt
    var systemPrompt: String
        get() = prefs.getString("system_prompt",
            "你是一个答题助手。请根据截图中的题目内容，给出正确的答案。"
            + "只回复答案内容，不需要解释。如果题目是选择题，直接给出选项字母。"
        ) ?: ""
        set(value) = prefs.edit().putString("system_prompt", value).apply()

    // 已配置的AI列表
    fun saveAiProviders(providers: List<AiProviderConfig>) {
        val json = gson.toJson(providers)
        prefs.edit().putString("ai_providers", json).apply()
    }

    fun getAiProviders(): List<AiProviderConfig> {
        val json = prefs.getString("ai_providers", "[]") ?: "[]"
        val type = object : TypeToken<List<AiProviderConfig>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addAiProvider(config: AiProviderConfig) {
        val list = getAiProviders().toMutableList()
        list.add(config)
        saveAiProviders(list)
    }

    fun removeAiProvider(name: String) {
        val list = getAiProviders().toMutableList()
        list.removeAll { it.name == name }
        saveAiProviders(list)
    }

    data class AiProviderConfig(
        val name: String = "",
        val apiUrl: String = "",
        val apiKey: String = "",
        val model: String = "",
        val enabled: Boolean = true
    )
}
