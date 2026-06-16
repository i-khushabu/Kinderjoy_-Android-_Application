package com.yourname.kidslearning

import android.media.MediaPlayer
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalContext



data class PairItem(val id: Int, val left: String, val right: String)

@Composable
fun MatchThePairScreen() {
    val fruits = listOf("🍎", "🍌", "🍇", "🍉", "🍍", "🍊", "🍓", "🥝")
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.correct) }

    val allPairs = remember {
        (1..8).map { num ->
            PairItem(
                id = num,
                left = num.toString(),
                right = List(num) { fruits[(num - 1) % fruits.size] }.joinToString(" ")
            )
        }.shuffled()
    }

    var currentBatchIndex by remember { mutableStateOf(0) }
    var matchedPairs by remember { mutableStateOf(setOf<Int>()) }
    var selectedLeft by remember { mutableStateOf<PairItem?>(null) }

    val sparkleVisible = remember { mutableStateOf(false) }

    val batchSize = 3
    val currentPairs = remember(currentBatchIndex) {
        allPairs.drop(currentBatchIndex * batchSize).take(batchSize)
    }

    LaunchedEffect(matchedPairs) {
        if (matchedPairs.size == currentPairs.size) {
            delay(1500)
            selectedLeft = null
            matchedPairs = emptySet()
            currentBatchIndex++
        }
    }
    val activity = LocalActivity.current
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.quizback),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
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
        // Sparkle Animation
        if (sparkleVisible.value) {
            val sparkleComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.sparkle))

            val sparkleAnimationState = animateLottieCompositionAsState(
                composition = sparkleComposition,
                iterations = 1,
                isPlaying = sparkleVisible.value
            )

            LaunchedEffect(sparkleAnimationState.isAtEnd && !sparkleAnimationState.isPlaying) {
                if (sparkleAnimationState.isAtEnd && !sparkleAnimationState.isPlaying) {
                    sparkleVisible.value = false
                }
            }

            LottieAnimation(
                composition = sparkleComposition,
                progress = { sparkleAnimationState.progress },
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.Center)
            )
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (currentPairs.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column {
                        currentPairs.forEach { pair ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickable(enabled = !matchedPairs.contains(pair.id)) {
                                        selectedLeft = pair
                                    }
                                    .background(
                                        if (selectedLeft?.id == pair.id) Color.LightGray else Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Text(text = pair.left, fontSize = 24.sp)
                            }
                        }
                    }

                    Column {
                        currentPairs.shuffled().forEach { pair ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickable(enabled = !matchedPairs.contains(pair.id)) {
                                        if (selectedLeft?.id == pair.id) {
                                            matchedPairs = matchedPairs + pair.id
                                            selectedLeft = null
                                            sparkleVisible.value = true
                                            mediaPlayer.start()
                                        }
                                    }
                                    .background(
                                        if (matchedPairs.contains(pair.id)) Color(0xFFC8E6C9) else Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Text(text = pair.right, fontSize = 24.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                if (matchedPairs.size == currentPairs.size) {
                    Text(
                        text = "Well done!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                Text(
                    text = "🎉 You matched all pairs!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }
        }

        // 🐵 Mascot on bottom-left
        Image(
            painter = painterResource(id = R.drawable.goat),
            contentDescription = "Mascot",
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMatchThePairScreen() {
    MatchThePairScreen()
}