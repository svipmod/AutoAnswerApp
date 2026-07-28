package com.autoanswer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.autoanswer.AutoAnswerApp
import com.autoanswer.ai.AiType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val prefs = AutoAnswerApp.instance.preferenceManager

    var selectedProvider by remember { mutableStateOf(prefs.aiProvider) }
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var apiUrl by remember { mutableStateOf(prefs.apiUrl) }
    var modelName by remember { mutableStateOf(prefs.modelName) }
    var answerDelay by remember { mutableStateOf(prefs.answerDelay.toString()) }
    var captureInterval by remember { mutableStateOf(prefs.captureInterval.toString()) }
    var antiSwitch by remember { mutableStateOf(prefs.antiSwitchEnabled) }
    var antiRecord by remember { mutableStateOf(prefs.antiRecordEnabled) }
    var systemPrompt by remember { mutableStateOf(prefs.systemPrompt) }
    var showApiKey by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        // 保存设置
                        prefs.aiProvider = selectedProvider
                        prefs.apiKey = apiKey
                        prefs.apiUrl = apiUrl
                        prefs.modelName = modelName
                        prefs.answerDelay = answerDelay.toIntOrNull() ?: 2000
                        prefs.captureInterval = captureInterval.toIntOrNull() ?: 3000
                        prefs.antiSwitchEnabled = antiSwitch
                        prefs.antiRecordEnabled = antiRecord
                        prefs.systemPrompt = systemPrompt
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAiDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加AI")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // AI 配置
            SectionTitle("🤖 AI 模型配置")

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("选择AI提供商", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))

                    // AI提供选择
                    val providers = AiType.entries.toList()
                    providers.forEach { type ->
                        FilterChip(
                            selected = selectedProvider == type.name,
                            onClick = { selectedProvider = type.name; apiUrl = type.defaultUrl },
                            label = { Text(type.displayName) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(if (showApiKey) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility, contentDescription = null)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("API 地址") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(selectedProvider.let {
                            AiType.valueOf(it).defaultUrl.ifEmpty { "https://..." }
                        }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("模型名称") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("gpt-4o / claude-3 / gemini-pro") }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 答题配置
            SectionTitle("⚙️ 答题设置")
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = answerDelay,
                        onValueChange = { answerDelay = it },
                        label = { Text("答题延迟(毫秒)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("识别题目后等待多久再回答，默认2000ms") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = captureInterval,
                        onValueChange = { captureInterval = it },
                        label = { Text("截图间隔(毫秒)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("每隔多久截一次屏，默认3000ms") }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 安全配置
            SectionTitle("🔒 安全监控")
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SwitchRow(
                        title = "防切屏检测",
                        desc = "检测切换到其他应用，超过3次触发警告",
                        checked = antiSwitch,
                        onCheckedChange = { antiSwitch = it },
                        icon = Icons.Default.PhoneAndroid
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow(
                        title = "防录屏检测",
                        desc = "检测屏幕录制状态，发现录屏时触发警告",
                        checked = antiRecord,
                        onCheckedChange = { antiRecord = it },
                        icon = Icons.Default.Videocam
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // System Prompt
            SectionTitle("📝 自定义提示词")
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        placeholder = { Text("输入AI的系统提示词...") }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("添加自定义AI") },
            text = {
                Column {
                    Text("支持任何兼容 OpenAI API 格式的服务")
                    Spacer(Modifier.height(8.dp))
                    Text("例如: ", style = MaterialTheme.typography.bodySmall)
                    Text("• 本地 ollama: http://192.168.1.100:11434/v1", style = MaterialTheme.typography.bodySmall)
                    Text("• 各种中转API", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiDialog = false }) { Text("知道了") }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(desc, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
