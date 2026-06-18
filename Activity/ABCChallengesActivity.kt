package com.yourname.kidslearning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex



class ABCChallengesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)

        setContent {
            ABCChallengesScreen { selectedGame ->
                navigateToGame(selectedGame)
            }
        }
    }

    override fun onDestroy() {
        AppSessionManager.cancelSession() // ✅ Only cancel the session
        super.onDestroy()
    }

    private fun navigateToGame(game: String) {
        val intent = when (game) {
            "FindLetter" -> Intent(this, FindLetterActivity::class.java)
            "PopBalloon" -> Intent(this, PopBalloonActivity::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }
}





@Composable
fun ABCChallengesScreen(navigateToGame: (String) -> Unit) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 🌄 Background
        Image(
            painter = painterResource(id = R.drawable.newchal),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 🔙 Back Button (top-right)
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

        // 🧩 Game Option Buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 16.dp, end = 16.dp), // top padding for back button
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Find the Letter Game
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clickable { navigateToGame("FindLetter") },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.find_letter_img),
                        contentDescription = "Find Letter",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                // Pop the Balloon Game
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clickable { navigateToGame("PopBalloon") },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pop_balloon_img),
                        contentDescription = "Pop Balloon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }
    }
}
