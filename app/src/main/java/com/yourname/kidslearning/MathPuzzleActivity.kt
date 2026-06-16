package com.yourname.kidslearning

import android.content.Intent
import android.os.Bundle
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer


class MathPuzzleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)


        setContent {
            MathLearningScreen { section ->
                navigateToMathSection(section)
            }
        }
    }
    override fun onDestroy() {


        AppSessionManager.cancelSession()
        super.onDestroy()
    }


    private fun navigateToMathSection(section: String) {
        val intent = when (section) {
            "Teaching" -> Intent(this, MathTeachingActivity::class.java)
            "Writing" -> Intent(this, MathWritingActivity::class.java)
            "Challenges" -> Intent(this, MathChallengesActivity::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }
}

@Composable
fun MathLearningScreen(navigateToMathSection: (String) -> Unit) {

    val activity = LocalActivity.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 🌄 Background
        Image(
            painter = painterResource(id = R.drawable.backs1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
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

        // 🧩 Slider Section
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(20.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(90.dp)) // Left space

                MathGameButton(R.drawable.teachmath, "Teaching") {
                    navigateToMathSection("Teaching")
                }

                MathGameButton(R.drawable.writingmath, "Writing") {
                    navigateToMathSection("Writing")
                }

                MathGameButton(R.drawable.challmath, "Challenges") {
                    navigateToMathSection("Challenges")
                }

                Spacer(modifier = Modifier.width(20.dp)) // Right space
            }
        }

    }
}

@Composable
fun MathGameButton(imageResId: Int, label: String, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            isPressed = true
            scope.launch {
                delay(150)
                isPressed = false
            }
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.6f),
            contentColor = Color.Black
        ),
        modifier = Modifier
            .width(180.dp)
            .height(170.dp)
            .padding(2.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = 22.sp,
                color = Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .padding(vertical = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMathLearningScreen() {
    MathLearningScreen { }
}
