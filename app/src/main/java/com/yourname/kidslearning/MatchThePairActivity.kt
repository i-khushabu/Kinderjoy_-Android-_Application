package com.yourname.kidslearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MatchThePairActivity : ComponentActivity() {
    private var sessionStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)
        setContent {
            MatchThePairScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartTime = System.currentTimeMillis()
    }

    override fun onDestroy() {
        val sessionEndTime = System.currentTimeMillis()
        val durationMillis = sessionEndTime - sessionStartTime
        val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1)
        saveProgressToFirebase("Numbers", durationMinutes)
        AppSessionManager.cancelSession()
        super.onDestroy()
    }
}
