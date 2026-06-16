package com.yourname.kidslearning

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.util.*

class MythologyQuizActivity : ComponentActivity() {
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
            MythologyQuizGame { message ->
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
        val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1)
        saveProgressToFirebase("Mythology", durationMinutes)
        AppSessionManager.cancelSession()
        super.onDestroy()
    }

    private fun speakOut(message: String) {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}

data class QuestionModel(val characterImage: Int, val correctOption: Int)
@Composable
fun MythologyQuizGame(onSpeak: (String) -> Unit) {
    val context = LocalContext.current

    val questions = listOf(
        QuestionModel(R.drawable.hanuman, R.drawable.gada),
        QuestionModel(R.drawable.krishna1, R.drawable.flute),
        QuestionModel(R.drawable.ram, R.drawable.bow),
        QuestionModel(R.drawable.ganesha1, R.drawable.mouse)
    )

    val options = listOf(
        Pair(R.drawable.flute, "Flute"),
        Pair(R.drawable.gada, "Gada"),
        Pair(R.drawable.bow, "Bow"),
        Pair(R.drawable.mouse, "Mushakraj")
    )

    val characterNames = listOf("Hanuman", "Krishna", "Ram", "Ganesha")

    var questionIndex by remember { mutableStateOf(0) }
    var feedbackMessage by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }
    var triggerNextQuestion by remember { mutableStateOf(false) }
    var triggerClearWrong by remember { mutableStateOf(false) }

    val currentQuestion = questions[questionIndex]
    val characterName = characterNames[questionIndex]

    LaunchedEffect(triggerNextQuestion) {
        if (triggerNextQuestion) {
            delay(1000)
            questionIndex = (questionIndex + 1) % questions.size
            showFeedback = false
            triggerNextQuestion = false
        }
    }

    LaunchedEffect(triggerClearWrong) {
        if (triggerClearWrong) {
            delay(1000)
            showFeedback = false
            triggerClearWrong = false
        }
    }

    val gradient = Brush.verticalGradient(colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFCCBC)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(16.dp)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .zIndex(1f),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = "Back",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = Color(0xFF6200EE),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { (context as? ComponentActivity)?.finish() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Find the Match",
                fontSize = 28.sp,
                color = Color(0xFF4E342E),
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Character with Name
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = characterName,
                        fontSize = 24.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(8.dp)
                    )
                    Image(
                        painter = painterResource(id = currentQuestion.characterImage),
                        contentDescription = "Character",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(300.dp),
                        contentScale = ContentScale.Fit
                    )

                }

                // Right: Options with Name
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showFeedback) {
                        Text(
                            text = feedbackMessage,
                            fontSize = 24.sp,
                            color = if (feedbackMessage == "Correct!") Color(0xFF2E7D32) else Color.Red
                        )
                    }

                    fun handleClick(option: Int) {
                        if (option == currentQuestion.correctOption) {
                            feedbackMessage = "Correct!"
                            onSpeak("Well done!")
                            showFeedback = true
                            triggerNextQuestion = true
                        } else {
                            feedbackMessage = "Wrong!"
                            onSpeak("Try again!")
                            showFeedback = true
                            triggerClearWrong = true
                        }
                    }

                    for (rowOptions in options.chunked(2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowOptions.forEach { (imageRes, label) ->
                                OptionButtonWithLabel(imageRes, label) { handleClick(imageRes) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionButtonWithLabel(imageRes: Int, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 🔼 Move label ABOVE the image
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(90.dp)
            )
        }
    }
}
