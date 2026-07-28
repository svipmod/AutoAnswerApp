package com.autoanswer.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume

class OcrEngine {

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    data class OcrResult(
        val text: String,
        val confidence: Float = 0f,
        val blocks: List<TextBlock> = emptyList()
    )

    data class TextBlock(
        val text: String,
        val left: Float, val top: Float,
        val right: Float, val bottom: Float
    )

    /** 识别图片中的文字 */
    suspend fun recognize(bitmap: Bitmap): OcrResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val text = recognizer.process(image).await()
            val blocks = text.textBlocks.mapNotNull { block ->
                val box = block.boundingBox ?: return@mapNotNull null
                TextBlock(
                    text = block.text,
                    left = box.left.toFloat(),
                    top = box.top.toFloat(),
                    right = box.right.toFloat(),
                    bottom = box.bottom.toFloat()
                )
            }
            OcrResult(text = text.text, blocks = blocks)
        } catch (e: Exception) {
            OcrResult(text = "", blocks = emptyList())
        }
    }

    /** 提取题目文本 */
    fun extractQuestions(ocrResult: OcrResult): List<String> {
        val lines = ocrResult.text.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 3 }
            .filterNot { it.matches(Regex("^[A-E][.、．]?\\s*$")) }
        return lines
    }

    /** 提取选项列表 */
    fun extractOptions(ocrResult: OcrResult): Map<String, String> {
        val options = mutableMapOf<String, String>()
        val regex = Regex("""^([A-E])[.、．]?\s*(.+)$""")
        ocrResult.text.split("\n").forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                options[match.groupValues[1]] = match.groupValues[2].trim()
            }
        }
        return options
    }
}

/** 将 Task 转为可等待的 suspend 函数 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
}
