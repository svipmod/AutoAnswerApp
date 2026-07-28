package com.autoanswer.ui

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autoanswer.AutoAnswerApp
import com.autoanswer.capture.ScreenCaptureService
import com.autoanswer.security.SecurityMonitorService
import com.autoanswer.ui.screens.*
import com.autoanswer.ui.theme.AutoAnswerTheme

class MainActivity : ComponentActivity() {

    private var screenCaptureResultCode by mutableIntStateOf(-1)
    private var screenCaptureData: Intent? = null

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            screenCaptureResultCode = result.resultCode
            screenCaptureData = result.data
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoAnswerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(
                        onRequestCapturePermission = { requestCapturePermission() },
                        capturePermissionGranted = screenCaptureResultCode != -1
                    )
                }
            }
        }
    }

    private fun requestCapturePermission() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = manager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }
}

@Composable
fun MainApp(
    onRequestCapturePermission: () -> Unit = {},
    capturePermissionGranted: Boolean = false
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAnswer = { navController.navigate("answer") },
                onNavigateToLogs = { navController.navigate("logs") },
                capturePermissionGranted = capturePermissionGranted,
                onRequestCapturePermission = onRequestCapturePermission
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("answer") {
            AnswerScreen(
                onBack = { navController.popBackStack() },
                capturePermissionGranted = capturePermissionGranted,
                onRequestCapturePermission = onRequestCapturePermission
            )
        }
        composable("logs") {
            LogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
