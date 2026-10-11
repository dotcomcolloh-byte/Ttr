package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.telefam.company.app.notifications.TelefamNotificationEngine
import com.telefam.company.app.ui.TelefamRootApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TelefamNotificationEngine.ensureChannelsCreated(applicationContext)
        TelefamNotificationEngine.handleIncomingActivityIntent(applicationContext, intent)
        enableEdgeToEdge()
        setContent {
            TelefamRootApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        TelefamNotificationEngine.handleIncomingActivityIntent(applicationContext, intent)
    }

    override fun onStart() {
        super.onStart()
        TelefamNotificationEngine.onAppForegroundStateChanged(applicationContext, true)
    }

    override fun onStop() {
        TelefamNotificationEngine.onAppForegroundStateChanged(applicationContext, false)
        super.onStop()
    }
}
