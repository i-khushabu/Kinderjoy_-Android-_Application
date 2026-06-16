package com.yourname.kidslearning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape

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
import androidx.compose.ui.zIndex


class ABCLearningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)

        setContent {
            ABCLearningScreen { section ->
                navigateToABCSection(section)
            }
        }
    }

    override fun onDestroy() {
        AppSessionManager.cancelSession() // ✅ Remove progress tracking here
        super.onDestroy()
    }

    private fun navigateToABCSection(section: String) {
        val intent = when (section) {
            "Teaching" -> Intent(this, ABCTeachingActivity::class.java)
            "Writing" -> Intent(this, ABCWritingActivity::class.java)
            "Challenges" -> Intent(this, ABCChallengesActivity::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }
}


@Composable
fun ABCLearningScreen(navigateToABCSection: (String) -> Unit) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 🌄 Background
        Image(
            painter = painterResource(id = R.drawable.abcback1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 🔙 Styled Back Button (top-right corner)
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
                    .background(Color(0xFF6200EE), shape = RoundedCornerShape(12.dp))
                    .clickable { (context as? ComponentActivity)?.finish() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 🧩 Game Section
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(10.dp)) // Left space

                ABCGameButton(R.drawable.abcteach, "Teaching") {
                    navigateToABCSection("Teaching")
                }

                ABCGameButton(R.drawable.abcwriting, "Writing") {
                    navigateToABCSection("Writing")
                }

                ABCGameButton(R.drawable.abctrophy, "Challenges") {
                    navigateToABCSection("Challenges")
                }

                Spacer(modifier = Modifier.width(10.dp)) // Right space
            }
        }
    }
}


@Composable
fun ABCGameButton(imageResId: Int, label: String, onClick: () -> Unit) {
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
fun PreviewABCLearningScreen() {
    ABCLearningScreen { }
}
