package com.autoanswer.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoanswer.AutoAnswerApp
import com.autoanswer.ai.AiProvider
import com.autoanswer.ai.OpenAiProviderImpl
import com.autoanswer.capture.ScreenCaptureService
import com.autoanswer.ocr.OcrEngine
import com.autoanswer.security.SecurityMonitorService
import kotlinx.coroutines.*

data class AnswerLog(
    val id: Long = System.currentTimeMillis(),
    val question: String = "",
    val answer: String = "",
    val timestamp: String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        .format(java.util.Date()),
    val success: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerScreen(
    onBack: () -> Unit = {},
    capturePermissionGranted: Boolean = false,
    onRequestCapturePermission: () -> Unit = {}
) {
    var isRunning by remember { mutableStateOf(false) }
    var currentQuestion by remember { mutableStateOf("") }
    var currentAnswer by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("就绪") }
    var logs by remember { mutableStateOf(listOf<AnswerLog>()) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var switchCount by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val prefs = AutoAnswerApp.instance.preferenceManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自动答题") },
                navigationIcon = {
                    IconButton(onClick = {
                        stopAnswer(scope)
                        onBack()
                    }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 状态栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip("截图", if (capturePermissionGranted) "就绪" else "未授权",
                    if (capturePermissionGranted) Color(0xFF4CAF50) else Color(0xFFFF9800))
                Spacer(Modifier.width(8.dp))
                StatusChip("AI", if (prefs.apiKey.isNotBlank()) "已配置" else "未配置",
                    if (prefs.apiKey.isNotBlank()) Color(0xFF4CAF50) else Color(0xFFFF9800))
                Spacer(Modifier.width(8.dp))
                StatusChip("安全", if (prefs.antiSwitchEnabled) "监控中" else "关闭",
                    if (prefs.antiSwitchEnabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
            }

            Spacer(Modifier.height(12.dp))

            // 状态信息
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Circle, contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF4CAF50))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(statusText, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 安全提示
            if (switchCount > 0 || isRecording) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isRecording) "⚠️ 检测到屏幕录制！"
                            else "⚠️ 已切屏 $switchCount 次",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 题目和答案
            if (currentQuestion.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("题目:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(currentQuestion, fontSize = 14.sp)
                        if (currentAnswer.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            Text("答案:", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(currentAnswer, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 答题记录
            Text("答题记录", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs.reversed()) { log ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (log.success)
                                MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row {
                                Text(log.timestamp, fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(if (log.success) "✅" else "❌", fontSize = 12.sp)
                            }
                            Text(log.question.take(80), fontSize = 13.sp,
                                maxLines = 2)
                            Text("→ ${log.answer}", fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!capturePermissionGranted) {
                    Button(
                        onClick = onRequestCapturePermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("授权截图")
                    }
                } else {
                    Button(
                        onClick = {
                            if (isRunning) stopAnswer(scope)
                            else startAnswer(scope, prefs) { log ->
                                logs = logs + log
                                currentQuestion = log.question
                                currentAnswer = log.answer
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (isRunning) ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error)
                        else ButtonDefaults.buttonColors()
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isRunning) "停止" else "开始答题")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, value: String, color: Color) {
    AssistChip(
        onClick = {},
        label = { Text("$label: $value", fontSize = 11.sp) },
        leadingIcon = {
            Icon(Icons.Default.Circle, contentDescription = null,
                modifier = Modifier.size(8.dp), tint = color)
        },
        modifier = Modifier.height(28.dp)
    )
}

private fun startAnswer(
    scope: CoroutineScope,
    prefs: com.autoanswer.util.PreferenceManager,
    onLog: (AnswerLog) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        val ocr = OcrEngine()
        val aiProvider = createAiProvider(prefs)
        var running = true

        while (running) {
            try {
                // 1. 截图
                val bitmap = ScreenCaptureService.getInstance()?.captureScreen()
                if (bitmap == null) {
                    delay(1000)
                    continue
                }

                // 2. OCR识别
                val ocrResult = ocr.recognize(bitmap)
                if (ocrResult.text.isBlank()) {
                    delay(prefs.captureInterval.toLong())
                    continue
                }

                // 3. 提取题目
                val questions = ocr.extractQuestions(ocrResult)
                if (questions.isEmpty()) {
                    delay(prefs.captureInterval.toLong())
                    continue
                }

                val questionText = questions.joinToString("\n")

                // 4. 转Base64
                val output = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
                val base64 = android.util.Base64.encodeToString(
                    output.toByteArray(), android.util.Base64.NO_WRAP
                )

                // 5. AI答题
                val answer = aiProvider.answer(questionText, base64)

                // 6. 记录
                onLog(AnswerLog(
                    question = questionText.take(100),
                    answer = answer.take(200),
                    success = !answer.startsWith("错误") && !answer.startsWith("网络错误")
                ))

                delay(prefs.answerDelay.toLong())

            } catch (e: Exception) {
                onLog(AnswerLog(
                    question = "异常",
                    answer = e.localizedMessage ?: "未知错误",
                    success = false
                ))
                delay(3000)
            }
        }
    }
}

private fun stopAnswer(scope: CoroutineScope) {
    scope.coroutineContext.cancelChildren()
}

private fun createAiProvider(prefs: com.autoanswer.util.PreferenceManager): AiProvider {
    return OpenAiProviderImpl(
        apiKey = prefs.apiKey,
        baseUrl = prefs.apiUrl.ifBlank { "https://api.openai.com/v1" },
        model = prefs.modelName.ifBlank { "gpt-4o" },
        systemPrompt = prefs.systemPrompt
    )
}
