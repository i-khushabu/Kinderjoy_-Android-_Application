package com.yourname.kidslearning

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.util.*

class MathTeachingActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private var sessionStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)

        // Initialize TTS
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US  // Set language to English (US)
                textToSpeech.setSpeechRate(0.8f)   // Adjust speed (0.8x slower)
            }
        }

        setContent {
            MathTeachingScreen { text ->
                speakText(text)  // Call AI Speech Synthesis
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartTime = System.currentTimeMillis()
    }

    private fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        val sessionEndTime = System.currentTimeMillis()
        val durationMillis = sessionEndTime - sessionStartTime
        val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1)
        saveProgressToFirebase("Numbers", durationMinutes)

        AppSessionManager.cancelSession()
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}
