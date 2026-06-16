package com.yourname.kidslearning

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex


import com.airbnb.lottie.compose.*
@Composable
fun MathTeachingScreen(speak: (String) -> Unit) {
    val mathConcepts = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    var currentConceptIndex by remember { mutableStateOf(0) }
    val currentConcept = mathConcepts[currentConceptIndex]

    val numberToImage = mapOf(
        "1" to R.drawable.apple,
        "2" to R.drawable.mango,
        "3" to R.drawable.orange,
        "4" to R.drawable.apple,
        "5" to R.drawable.mango,
        "6" to R.drawable.apple,
        "7" to R.drawable.mango
    )

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.mascot_animation)
    )
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    val fruitResId = numberToImage[currentConcept] ?: R.drawable.apple
    val count = currentConcept.toIntOrNull() ?: 1

    val activity = LocalActivity.current


    Box(modifier = Modifier.fillMaxSize()) {

        // 🔙 Back Button
        Box(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .zIndex(1f)
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

        // Background
        Image(
            painter = painterResource(id = R.drawable.teachingback),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Main content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mascot
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(240.dp)
            )

            // Number & Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clickable { speak(currentConcept) }
                        .background(Color(0xFFFFF176), RoundedCornerShape(20.dp))
                        .size(width = 130.dp, height = 130.dp) // Fixed square shape
                ) {
                    // Centered Number
                    Text(
                        text = currentConcept,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF6C00),
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Top-Right Speaker Icon
                    Image(
                        painter = painterResource(id = R.drawable.speaker_icon), // Replace with your speaker icon
                        contentDescription = "Speaker Icon",
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                    )
                }



                Button(
                    onClick = {
                        currentConceptIndex = (currentConceptIndex + 1) % mathConcepts.size
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("➡ Next", fontSize = 16.sp, color = Color.White)
                }
            }

            // Falling Fruits
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(250.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                val columns = 3
                val itemSize = 50.dp
                val spacing = 6.dp

                repeat(count) { index ->
                    var offsetY by remember { mutableStateOf(-200f) }

                    LaunchedEffect(currentConcept, index) {
                        kotlinx.coroutines.delay(index * 100L)
                        animate(
                            initialValue = -200f,
                            targetValue = (index / columns) * 70f,
                            animationSpec = tween(durationMillis = 800)
                        ) { value, _ ->
                            offsetY = value
                        }
                    }

                    val col = index % columns
                    val xOffset = itemSize * col + spacing * col

                    Image(
                        painter = painterResource(id = fruitResId),
                        contentDescription = "Falling Object $index",
                        modifier = Modifier
                            .size(itemSize)
                            .offset(x = xOffset, y = offsetY.dp)
                    )
                }
            }
        }
    }
}

