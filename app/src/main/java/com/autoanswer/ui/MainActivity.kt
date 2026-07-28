package com.autoanswer.ui

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    var captureGranted by remember { mutableStateOf(false) }
    val activity = LocalActivity.current

    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK && result.data != null) {
            captureGranted = true
            activity?.let { act ->
                ScreenCaptureService.start(act, result.resultCode, result.data!!)
            }
        }
    }

    fun requestScreenCapture() {
        val manager = activity?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (manager != null) {
            val intent = manager.createScreenCaptureIntent()
            captureLauncher.launch(intent)
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAnswer = { navController.navigate("answer") },
                onNavigateToLogs = { navController.navigate("logs") },
                capturePermissionGranted = captureGranted,
                onRequestCapturePermission = { requestScreenCapture() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("answer") {
            AnswerScreen(
                onBack = { navController.popBackStack() },
                capturePermissionGranted = captureGranted,
                onRequestCapturePermission = { requestScreenCapture() }
            )
        }
        composable("logs") {
            LogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
