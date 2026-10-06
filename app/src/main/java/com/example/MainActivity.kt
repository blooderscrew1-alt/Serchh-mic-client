package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.ClientSettings
import com.example.network.RemoteClientHolder
import com.example.receiver.ScheduledRebootManager
import com.example.ui.SearchMicClientApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ZenOledBlack
import com.example.voice.RemoteMicForegroundService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure immersive full-screen edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        // Configure lockscreen display and screen wake upon startup/boot
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        val settings = ClientSettings(this)
        if (settings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Auto-connect if configured
        if (settings.autoConnectOnLaunch && settings.hostIp.isNotBlank()) {
            RemoteClientHolder.connect(this)
        }

        // Synchronize daily reboot maintenance schedule
        ScheduledRebootManager.scheduleDailyReboot(this)

        setContent {
            MyApplicationTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    if (grants[Manifest.permission.RECORD_AUDIO] == true && settings.autoStartListeningOnLaunch) {
                        RemoteMicForegroundService.start(this@MainActivity)
                    }
                }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    val hasAudio = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (!hasAudio) {
                        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    } else if (settings.autoStartListeningOnLaunch) {
                        RemoteMicForegroundService.start(this@MainActivity)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ZenOledBlack
                ) {
                    SearchMicClientApp(
                        onStartService = {
                            RemoteMicForegroundService.start(this@MainActivity)
                        },
                        onStopService = {
                            RemoteMicForegroundService.stop(this@MainActivity)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

