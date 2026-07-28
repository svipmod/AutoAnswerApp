package com.autoanswer.security

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.autoanswer.R
import java.io.File

class SecurityMonitorService : Service() {

    private var isMonitoring = false
    private var switchCount = 0
    private var lastForegroundPackage = ""
    private var mediaRecorder: MediaRecorder? = null
    private var isScreenRecording = false

    private var onSwitchDetected: ((Int) -> Unit)? = null
    private var onRecordDetected: ((Boolean) -> Unit)? = null

    companion object {
        const val ACTION_START = "com.autoanswer.action.START_SECURITY"
        const val ACTION_STOP = "com.autoanswer.action.STOP_SECURITY"
        const val CHANNEL_ID = "security_monitor_channel"
        const val NOTIFICATION_ID = 1002
        const val MAX_SWITCH_COUNT = 3 // 最大允许切屏次数

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, SecurityMonitorService::class.java).apply {
                    action = ACTION_START
                }
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, SecurityMonitorService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        isMonitoring = true
        switchCount = 0
        lastForegroundPackage = packageName
        startScreenRecordingDetection()
    }

    private fun stopMonitoring() {
        isMonitoring = false
        stopScreenRecordingDetection()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── 防切屏检测 ──

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // 息屏 = 切屏
                    onSwitchDetected()
                }
                Intent.ACTION_USER_PRESENT -> {
                    // 亮屏后检查前台应用
                    checkForegroundApp()
                }
            }
        }
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    private fun checkForegroundApp() {
        if (!isMonitoring) return
        try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 1000 * 60,
                currentTime
            )
            if (stats.isNotEmpty()) {
                val topPackage = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return
                if (topPackage != packageName && topPackage != "com.android.systemui") {
                    onSwitchDetected()
                }
            }
        } catch (e: SecurityException) {
            // 无使用权限
        }
    }

    private fun onSwitchDetected() {
        switchCount++
        onSwitchDetected?.invoke(switchCount)
        if (switchCount >= MAX_SWITCH_COUNT) {
            // 触发警告或锁定
            notifySwitchExceeded()
        }
    }

    private fun notifySwitchExceeded() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ 切屏次数过多")
            .setContentText("已切屏 $switchCount 次，可能被判定为作弊")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1003, notification)
    }

    // ── 防录屏检测 ──

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            checkScreenRecording()
        }
    }

    private fun startScreenRecordingDetection() {
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        // 检查当前是否有录屏
        checkScreenRecording()
    }

    private fun stopScreenRecordingDetection() {
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        try {
            displayManager.unregisterDisplayListener(displayListener)
        } catch (_: Exception) {}
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun checkScreenRecording() {
        try {
            // 方法1: 检查显示密度变化（录屏时可能变化）
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(metrics)

            // 方法2: 尝试创建 MediaRecorder 检测
            val tempFile = File(cacheDir, "record_test.tmp")
            val recorder = MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(tempFile.absolutePath)
            recorder.prepare()
            recorder.start()
            isScreenRecording = false
            recorder.stop()
            recorder.release()
            tempFile.delete()

            onRecordDetected?.invoke(false)
        } catch (e: Exception) {
            // 无法创建 MediaRecorder = 可能正在被录屏占用
            isScreenRecording = true
            onRecordDetected?.invoke(true)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⚠️ 检测到屏幕录制")
                .setContentText("请关闭录屏功能以确保考试安全")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(1004, notification)
        }
    }

    fun getSwitchCount(): Int = switchCount

    fun setSwitchListener(listener: (Int) -> Unit) {
        onSwitchDetected = listener
    }

    fun setRecordListener(listener: (Boolean) -> Unit) {
        onRecordDetected = listener
    }

    override fun onDestroy() {
        isMonitoring = false
        try { unregisterReceiver(screenStateReceiver) } catch (_: Exception) {}
        stopScreenRecordingDetection()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "安全监控",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "防切屏防录屏监控" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 安全监控")
            .setContentText("防切屏/防录屏已开启")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }
}
