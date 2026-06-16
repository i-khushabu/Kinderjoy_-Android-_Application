package com.yourname.kidslearning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent


class MathChallengesActivity : ComponentActivity() {
    private var sessionStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)

        setContent {
            MathChallengesScreen { challenge ->
                when (challenge) {
                    "Match The Pair" -> startActivity(Intent(this, MatchThePairActivity::class.java))
                    "Train Game" -> startActivity(Intent(this, TrainGameActivity::class.java))
                }

            }

        }
    }
    override fun onStart() {
        super.onStart()
        sessionStartTime = System.currentTimeMillis()
    }
    override fun onDestroy() {

        super.onDestroy()
        AppSessionManager.cancelSession()
    }
}

