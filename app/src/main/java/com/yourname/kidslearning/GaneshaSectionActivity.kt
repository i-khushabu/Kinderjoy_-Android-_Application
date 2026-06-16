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
import java.util.*

class GaneshaSectionActivity : ComponentActivity() {

    private var sessionStartTime: Long = 0
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.ENGLISH
            }
        }

        setContent {
            GaneshaSectionScreen(tts)
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
        saveProgressToFirebase("Mythology", durationMinutes)

        tts.stop()
        tts.shutdown()

        super.onDestroy()
        AppSessionManager.cancelSession()
    }
}

data class DivineCharacter(
    val name: String,
    val description: String,
    val imageResId: Int
)

@Composable
fun GaneshaSectionScreen(tts: TextToSpeech) {
    val context = LocalContext.current

    val characters = listOf(
        DivineCharacter("Ganesha", "The elephant-headed god of wisdom, prosperity and new beginnings.", R.drawable.ganesha1),
        DivineCharacter("Kartikeya", "The brave god of war and commander of the divine army.", R.drawable.kartikeya),
        DivineCharacter("Shiva", "The supreme yogi and destroyer of evil, father of Ganesha.", R.drawable.mahadev),
        DivineCharacter("Parvati", "The divine mother goddess, symbol of love and strength.", R.drawable.parvati),
        DivineCharacter("Mushakraj", "The loyal mouse who serves as Ganesha's vehicle and friend.", R.drawable.mushakraj)
    )

    var currentIndex by remember { mutableStateOf(0) }
    val currentCharacter = characters[currentIndex]

    // Speak character name when it changes
    LaunchedEffect(currentIndex) {
        tts.speak(currentCharacter.name, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFFC371), Color(0xFFFF5F6D)) // soft orange to pink
                )
            )
    ) {
        // ✅ Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(1f),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = "Back",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = Color(0xFF6A1B9A),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        (context as? ComponentActivity)?.finish()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ✅ Main Content (pushed down)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Image(
                painter = painterResource(id = currentCharacter.imageResId),
                contentDescription = currentCharacter.name,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = currentCharacter.name,
                    fontSize = 36.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = currentCharacter.description,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = {
                        currentIndex = (currentIndex + 1) % characters.size
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(text = "Next", color = Color(0xFF6A1B9A), fontSize = 18.sp)
                }
            }
        }
    }
}

