package com.yourname.kidslearning

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.widget.Toast
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.*

class ABCTeachingActivity : ComponentActivity() {
    private var sessionStartTime: Long = 0
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private val REQUEST_CODE_MICROPHONE = 1

    // Add state variables for feedback
    private var currentSelectedLetter = "A"
    private var feedbackMessage by mutableStateOf("")
    private var showFeedback by mutableStateOf(false)
    private var isCorrectAnswer by mutableStateOf(false)

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_MICROPHONE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start speech recognition
                startSpeechRecognition()
            } else {
                // Permission denied, show a toast message
                Toast.makeText(this, "Microphone permission is required for speech recognition", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check for microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_MICROPHONE
            )
        }
        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                feedbackMessage = "Listening..."
                showFeedback = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(p0: Float) {}

            override fun onBufferReceived(p0: ByteArray?) {}

            override fun onEndOfSpeech() {
                feedbackMessage = "Processing..."
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech input"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }
                feedbackMessage = "Try again!"
                showFeedback = true
                isCorrectAnswer = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val recognizedText = matches[0].lowercase()  // Get the first recognized word
                    checkPronunciation(recognizedText, currentSelectedLetter)
                }
            }

            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        })

        enableImmersiveMode(window)
        setContent {
            ABCTeachingScreen(
                onLetterClick = { letter ->
                    currentSelectedLetter = letter
                    speakLetter(letter)
                    // Reset feedback when a new letter is selected
                    showFeedback = false
                },
                startSpeechRecognition = { startSpeechRecognition() },
                showFeedback = showFeedback,
                feedbackMessage = feedbackMessage,
                isCorrectAnswer = isCorrectAnswer,
                selectedLetter = currentSelectedLetter
            )
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartTime = System.currentTimeMillis()
    }

    // Function to start speech recognition
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say $currentSelectedLetter for ${letterMap[currentSelectedLetter]}")

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakLetter(letter: String) {
        val word = letterMap[letter] ?: ""
        val phrase = "$letter for $word"
        textToSpeech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Function to check pronunciation - Updated version
    private fun checkPronunciation(recognizedText: String, currentLetter: String) {
        val expectedWord = letterMap[currentLetter]?.lowercase() ?: ""
        val expectedPattern = "$currentLetter for $expectedWord".lowercase()
        val expectedLetterOnly = currentLetter.lowercase()

        // Check if any other letter-word combination is being said
        var incorrectLetterWordPair = false

        // Loop through all letters to check if user said a different letter-word combination
        letterMap.forEach { (letter, word) ->
            if (letter != currentLetter) {
                val otherPattern = "$letter for $word".lowercase()
                val otherPatternVariation = "$letter for ${word.lowercase()}"

                // Check if user said another letter's pattern
                if (recognizedText.contains(otherPattern) ||
                    recognizedText.contains(otherPatternVariation)) {
                    incorrectLetterWordPair = true
                    return@forEach
                }

                // Check if user mixed current letter with another word
                val mixedPattern1 = "$currentLetter for $word".lowercase()
                if (recognizedText.contains(mixedPattern1)) {
                    incorrectLetterWordPair = true
                    return@forEach
                }

                // Check if user mixed another letter with the current word
                val mixedPattern2 = "$letter for $expectedWord".lowercase()
                if (recognizedText.contains(mixedPattern2)) {
                    incorrectLetterWordPair = true
                    return@forEach
                }
            }
        }

        // First, check if any incorrect letter-word combination was detected
        if (incorrectLetterWordPair) {
            feedbackMessage = "That's not quite right. Try again!"
            isCorrectAnswer = false
        }
        // Then check if the recognized text contains the correct combination
        else if (recognizedText.contains(expectedLetterOnly) &&
            (recognizedText.contains(expectedWord) || recognizedText.contains(expectedPattern))) {
            feedbackMessage = "Great job!"
            isCorrectAnswer = true
        }
        else {
            feedbackMessage = "Try again!"
            isCorrectAnswer = false
        }

        showFeedback = true
    }


    override fun onDestroy() {
        val sessionEndTime = System.currentTimeMillis()
        val durationMillis = sessionEndTime - sessionStartTime
        val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1)
        saveProgressToFirebase("Alphabets", durationMinutes)

        super.onDestroy()

        // Clean up resources
        AppSessionManager.cancelSession()
        textToSpeech.stop()
        textToSpeech.shutdown()
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
    }
}

// Mapping Letters to Words and Images
val letterMap = mapOf(
    "A" to "Apple",
    "B" to "Ball",
    "C" to "Cat",
    "D" to "Dog",
    "E" to "Elephant",
    "F" to "Fish",
    "G" to "Goat",
    "H" to "Hat",
    "I" to "Ice Cream",
    "J" to "Jug",
    "K" to "Kite",
    "L" to "Lion",
    "M" to "Mango",
    "N" to "Nest",
    "O" to "Orange",
    "P" to "Parrot",
    "Q" to "Queen",
    "R" to "Rabbit",
    "S" to "Sun",
    "T" to "Tiger",
    "U" to "Umbrella",
    "V" to "Van",
    "W" to "Watch",
    "X" to "Xylophone",
    "Y" to "Yacht",
    "Z" to "Zebra"
)

// Mapping Letters to Image Resources
val letterImages = mapOf(
    "A" to R.drawable.apple,
    "B" to R.drawable.ball,
    "C" to R.drawable.cat,
    "D" to R.drawable.dog,
    "E" to R.drawable.elephant,
    "F" to R.drawable.fish,
    "G" to R.drawable.goat,
    "H" to R.drawable.hat,
    "I" to R.drawable.ice_cream,
    "J" to R.drawable.jug,
    "K" to R.drawable.kite,
    "L" to R.drawable.lion,
    "M" to R.drawable.mango,
    "N" to R.drawable.nest,
    "O" to R.drawable.orange,
    "P" to R.drawable.parrot,
    "Q" to R.drawable.queen,
    "R" to R.drawable.rabbit,
    "S" to R.drawable.sun,
    "T" to R.drawable.tiger,
    "U" to R.drawable.umbrella,
    "V" to R.drawable.van,
    "W" to R.drawable.watch,
    "X" to R.drawable.xylophone,
    "Y" to R.drawable.yacht,
    "Z" to R.drawable.zebra
)

@Composable
fun ABCTeachingScreen(
    onLetterClick: (String) -> Unit,
    startSpeechRecognition: () -> Unit,
    showFeedback: Boolean,
    feedbackMessage: String,
    isCorrectAnswer: Boolean,
    selectedLetter: String
) {
    var currentLetter by remember { mutableStateOf(selectedLetter) }

    // Update the current letter when selectedLetter changes
    LaunchedEffect(selectedLetter) {
        currentLetter = selectedLetter
    }

    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(currentLetter) {
        scaleAnim.animateTo(1.2f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        onLetterClick(currentLetter)
    }

    // Animation for feedback message
    val offsetAnim = remember { Animatable(0f) }

    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            offsetAnim.snapTo(-100f)
            offsetAnim.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
    }

    // 🔙 Back Button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4A90E2), Color(0xFF145DA0))
                ))
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        val context = LocalContext.current
        Text(
            text = "Back",
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier
                .background(Color(0xFF6200EE), shape = RoundedCornerShape(12.dp))
                .clickable { (context as? ComponentActivity)?.finish() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )


        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT SIDE
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.6f)
                        .padding(start = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentLetter,
                        fontSize = 72.sp,
                        color = Color.Yellow,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    letterImages[currentLetter]?.let { imageId ->
                        Image(
                            painter = painterResource(id = imageId),
                            contentDescription = "$currentLetter image",
                            modifier = Modifier
                                .size(150.dp)
                                .scale(scaleAnim.value)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$currentLetter for ${letterMap[currentLetter]}",
                        fontSize = 26.sp,
                        color = Color.White
                    )
                }

                // RIGHT SIDE - Letters Box
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feedback message at the top of right column
                    if (showFeedback) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .offset(y = offsetAnim.value.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrectAnswer) Color(0xFF4CAF50) else Color(0xFFFF5722)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = feedbackMessage,
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Alphabet selection card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val chunks = letterMap.keys.toList().chunked(7)
                            chunks.forEach { rowLetters ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    rowLetters.forEach { letter ->
                                        Text(
                                            text = letter,
                                            fontSize = 20.sp,
                                            color = if (currentLetter == letter) Color.Yellow else Color.White,
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp)
                                                .clickable { currentLetter = letter }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Speak Now button below the letters box
                    Button(
                        onClick = { startSpeechRecognition() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Speak Now",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}