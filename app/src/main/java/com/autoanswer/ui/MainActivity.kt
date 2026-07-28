package com.autoanswer.ui

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autoanswer.capture.ScreenCaptureService
import com.autoanswer.ui.screens.*
import com.autoanswer.ui.theme.AutoAnswerTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_SCREEN_CAPTURE = 1001
        var captureResult: Pair<Int, Intent?>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AutoAnswerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }

    fun requestScreenCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
    }

    @Deprecated("Use registerForActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            captureResult = Pair(resultCode, data)
            ScreenCaptureService.start(this, resultCode, data)
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    var captureGranted by remember { mutableStateOf(MainActivity.captureResult != null) }
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity

    // 监听 captureResult 变化
    LaunchedEffect(MainActivity.captureResult) {
        captureGranted = MainActivity.captureResult != null
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAnswer = { navController.navigate("answer") },
                onNavigateToLogs = { navController.navigate("logs") },
                capturePermissionGranted = captureGranted,
                onRequestCapturePermission = { activity?.requestScreenCapture() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("answer") {
            AnswerScreen(
                onBack = { navController.popBackStack() },
                capturePermissionGranted = captureGranted,
                onRequestCapturePermission = { activity?.requestScreenCapture() }
            )
        }
        composable("logs") {
            LogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
