package com.yourname.kidslearning

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import java.util.*

class ShapeRecognitionActivity : ComponentActivity() {
    private var sessionStartTime: Long = 0
    private lateinit var tts: TextToSpeech


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)


        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        setContent {
            ShapeRecognitionQuiz { message ->
                speakOut(message)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartTime = System.currentTimeMillis()
    }
    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        val sessionEndTime = System.currentTimeMillis()
        val durationMillis = sessionEndTime - sessionStartTime
        val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1) // Minimum 1 min
        saveProgressToFirebase("Shapes", durationMinutes) // or "ABC"

        // ✅ Cancel timer on exit
        AppSessionManager.cancelSession()

        super.onDestroy()
    }

    private fun speakOut(message: String) {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }


    data class ShapeQuestion(
        val correctShape: String,
        val options: List<String>
    )

    val questions = listOf(
        ShapeQuestion("circle", listOf("circle", "square", "triangle", "rectangle")),
        ShapeQuestion("square", listOf("triangle", "circle", "square", "rectangle")),
        ShapeQuestion("triangle", listOf("rectangle", "circle", "square", "triangle")),
        ShapeQuestion("star", listOf("star", "triangle", "circle", "square")),
        ShapeQuestion("pentagon", listOf("pentagon", "rectangle", "circle", "triangle")),
        ShapeQuestion("hexagon", listOf("hexagon", "square", "triangle", "circle"))
    )

    @Composable
    fun ShapeRecognitionQuiz(onSpeak: (String) -> Unit) {
        var currentQuestionIndex by remember { mutableStateOf(0) }
        var feedbackMessage by remember { mutableStateOf("") }
        var triggerNextQuestion by remember { mutableStateOf(false) }
        var shouldClearWrong by remember { mutableStateOf(false) }

        val currentQuestion = questions[currentQuestionIndex]
        val context = LocalContext.current

        LaunchedEffect(currentQuestionIndex) {
            onSpeak(currentQuestion.correctShape)
            feedbackMessage = ""
        }

        LaunchedEffect(triggerNextQuestion) {
            if (triggerNextQuestion) {
                kotlinx.coroutines.delay(1000)
                currentQuestionIndex = (currentQuestionIndex + 1) % questions.size
                feedbackMessage = ""
                triggerNextQuestion = false
            }
        }

        LaunchedEffect(shouldClearWrong) {
            if (shouldClearWrong) {
                kotlinx.coroutines.delay(1000)
                feedbackMessage = ""
                shouldClearWrong = false
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // Background image
            Image(
                painter = painterResource(id = R.drawable.desert),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Back button (top-right corner)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = "Back",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF6200EE), // Deep Purple color (you can change)
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            (context as? ComponentActivity)?.finish()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Find the matching shape!",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                if (feedbackMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = feedbackMessage,
                        fontSize = 28.sp,
                        color = if (feedbackMessage == "Correct!") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .size(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DrawShapeImage(currentQuestion.correctShape, size = 200.dp)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            currentQuestion.options.take(2).forEach { option ->
                                ShapeOptionButton(
                                    shape = option,
                                    correctShape = currentQuestion.correctShape,
                                    onCorrect = {
                                        feedbackMessage = "Correct!"
                                        onSpeak("Well done!")
                                        triggerNextQuestion = true
                                    },
                                    onWrong = {
                                        feedbackMessage = "Wrong!"
                                        onSpeak("Try again!")
                                        shouldClearWrong = true
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            currentQuestion.options.drop(2).forEach { option ->
                                ShapeOptionButton(
                                    shape = option,
                                    correctShape = currentQuestion.correctShape,
                                    onCorrect = {
                                        feedbackMessage = "Correct!"
                                        onSpeak("Well done!")
                                        triggerNextQuestion = true
                                    },
                                    onWrong = {
                                        feedbackMessage = "Wrong!"
                                        onSpeak("Try again!")
                                        shouldClearWrong = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ShapeOptionButton(
        shape: String,
        correctShape: String,
        onCorrect: () -> Unit,
        onWrong: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(width = 100.dp, height = 100.dp)
                .clickable {
                    if (shape == correctShape) {
                        onCorrect()
                    } else {
                        onWrong()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            DrawShapeImage(shape)
        }
    }

    @Composable
    fun DrawShapeImage(shape: String, size: Dp = 80.dp) {
        val imageRes = when (shape.lowercase()) {
            "circle" -> R.drawable.circle
            "square" -> R.drawable.square
            "triangle" -> R.drawable.triangle
            "rectangle" -> R.drawable.rectangle
            "star" -> R.drawable.star
            "pentagon" -> R.drawable.pentagon
            "hexagon" -> R.drawable.hexagon
            else -> R.drawable.circle
        }

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = shape,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
    }
}