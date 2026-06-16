package com.yourname.kidslearning

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale


import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.*

class FindLetterActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private var sessionStartTime: Long = 0  // ✅ Added

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        enableImmersiveMode(window)

        setContent {
            FindLetterScreen(this, textToSpeech)
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartTime = System.currentTimeMillis()  // ✅ Added
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()

        val sessionEndTime = System.currentTimeMillis()
        val durationMillis = sessionEndTime - sessionStartTime
        val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1)
        saveProgressToFirebase("Alphabets", durationMinutes)  // ✅ Consistent label

        AppSessionManager.cancelSession()
        super.onDestroy()
    }
}


@Composable
fun FindLetterScreen(context: Context, textToSpeech: TextToSpeech) {
    val words = listOf(
        Pair("Apple", R.drawable.apple),
        Pair("Ball", R.drawable.ball),
        Pair("Cat", R.drawable.cat),
        Pair("Dog", R.drawable.dog),
        Pair("Elephant", R.drawable.elephant),
        Pair("Fish", R.drawable.fish),
        Pair("Goat", R.drawable.goat),
        Pair("Hat", R.drawable.hat),
        Pair("Ice-Cream", R.drawable.ice_cream),
        Pair("Jug", R.drawable.jug),
        Pair("Kite", R.drawable.kite),
        Pair("Lion", R.drawable.lion),
        Pair("Mango", R.drawable.mango),
        Pair("Nest", R.drawable.nest),
        Pair("Orange", R.drawable.orange),
        Pair("Parrot", R.drawable.parrot),
        Pair("Queen", R.drawable.queen),
        Pair("Rabbit", R.drawable.rabbit),
        Pair("Sun", R.drawable.sun),
        Pair("Tiger", R.drawable.tiger),
        Pair("Umbrella", R.drawable.umbrella),
        Pair("Van", R.drawable.van),
        Pair("Watch", R.drawable.watch),
        Pair("Xylophone", R.drawable.xylophone),
        Pair("Yacht", R.drawable.yacht),
        Pair("Zebra", R.drawable.zebra)
    )
    var currentWord by remember { mutableStateOf(words.random()) }
    var isAnswered by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var stars by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(5) }

    val coroutineScope = rememberCoroutineScope()

    val correctLetter = currentWord.first[0].toString()
    val alphabet = ('A'..'Z').toList().filter { it.toString() != correctLetter }
    val wrongLetters = remember(currentWord) { alphabet.shuffled().take(2) }
    val letterOptions = remember(currentWord) { (listOf(correctLetter) + wrongLetters).shuffled() }

    // Timer Effect
    LaunchedEffect(currentWord) {
        isAnswered = false
        textToSpeech.speak(currentWord.first, TextToSpeech.QUEUE_FLUSH, null, null)
        timeLeft = 5
        while (timeLeft > 0 && !isAnswered) {
            delay(1000)
            timeLeft -= 1
        }
    }

    // Bounce animation
    val infiniteTransition = rememberInfiniteTransition()
    val imageBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val activity = (context as? ComponentActivity)
    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Background image
        Image(
            painter = painterResource(id = R.drawable.findletterback), // 🔁 Replace with your background image
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 🔙 Back Button
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
                    .background(Color(0xFF6200EE), shape = RoundedCornerShape(12.dp))
                    .clickable {
                        activity?.finish()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        // Foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ✅ Main word only
            Text(
                text = currentWord.first,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = "Score: $score  ⭐: $stars", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)

//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(text = "⏳ Time Left: $timeLeft seconds", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Red)

            Spacer(modifier = Modifier.height(11.dp))

            Image(
                painter = painterResource(id = currentWord.second),
                contentDescription = "Object",
                modifier = Modifier
                    .size(170.dp)
                    .offset(y = imageBounce.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                letterOptions.forEach { letter ->
                    Button(
                        onClick = {
                            if (!isAnswered) {
                                isAnswered = true
                                if (letter == correctLetter) {
                                    MediaPlayer.create(context, R.raw.correct).start()
                                    Toast.makeText(context, "Correct! ✅", Toast.LENGTH_SHORT).show()
                                    textToSpeech.speak("Great job!", TextToSpeech.QUEUE_FLUSH, null, null)
                                    score += 10
                                    if (timeLeft > 0) stars += 1
                                } else {
                                    MediaPlayer.create(context, R.raw.failure).start()
                                    Toast.makeText(context, "Try Again! ❌", Toast.LENGTH_SHORT).show()
                                }

                                coroutineScope.launch {
                                    delay(1500)
                                    currentWord = words.random()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                    ) {
                        Text(text = letter.toString(), fontSize = 24.sp, color = Color.White)
                    }
                }
            }
        }
    }
}