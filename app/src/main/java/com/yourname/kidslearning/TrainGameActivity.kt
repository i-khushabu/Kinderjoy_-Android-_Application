package com.yourname.kidslearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.media.MediaPlayer
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class TrainGameActivity : ComponentActivity() {
    private var sessionStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)
        setContent {
            MaterialTheme {
                TrainGameScreen()
            }
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
        saveProgressToFirebase("Numbers", durationMinutes)
        AppSessionManager.cancelSession()
        super.onDestroy()
    }
}


@Composable
fun TrainGameScreen() {

    val context = LocalContext.current
    var resetRequested by remember { mutableStateOf(false) }

    // Generate random sequences of 4 consecutive numbers
    fun generateRandomSequence(): List<Int> {
        val startNumber = Random.nextInt(1, 8) // 1-7 to ensure we can have 4 consecutive numbers up to 10
        return listOf(startNumber, startNumber + 1, startNumber + 2, startNumber + 3)
    }

    // Game state
    var selectedNumber by remember { mutableStateOf<Int?>(null) }
    var currentSequence by remember { mutableStateOf(generateRandomSequence()) }
    var coachNumbers by remember {
        mutableStateOf(mutableMapOf<Int, Int?>(
            0 to null,
            1 to null,
            2 to currentSequence[2], // Fixed number (3rd in sequence)
            3 to null
        ))
    }
    var availableNumbers by remember {
        mutableStateOf(listOf(currentSequence[0], currentSequence[1], currentSequence[3]))
    }
    var gameCompleted by remember { mutableStateOf(false) }
    var trainPosition by remember { mutableStateOf(800f) } // Start from far right
    var animationEnabled by remember { mutableStateOf(true) }
    var showSteam by remember { mutableStateOf(false) }

    // Animation for train movement
    val animatedPosition by animateFloatAsState(
        targetValue = trainPosition,
        animationSpec = if (animationEnabled) {
            tween(durationMillis = 2000, easing = LinearEasing)
        } else {
            snap() // Instant animation when disabled
        },
        label = "train_movement"
    )

    // Steam animation
    val steamOpacity by animateFloatAsState(
        targetValue = if (showSteam) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "steam_animation"
    )

    // Initialize train position
    LaunchedEffect(Unit) {
        delay(500)
        trainPosition = 0f // Move to center
    }

    // Check if game is completed
    LaunchedEffect(coachNumbers.values.toList()) {


        val isComplete = coachNumbers[0] == currentSequence[0] &&
                coachNumbers[1] == currentSequence[1] &&
                coachNumbers[2] == currentSequence[2] &&
                coachNumbers[3] == currentSequence[3]

        if (isComplete && !gameCompleted) {
            gameCompleted = true
            showSteam = true

            // Play train whistle sound (you'll need to add this method)
            // playTrainWhistleSound()

            playTrainWhistleSound(context)

            delay(1000)
            trainPosition = -800f // Move train completely off screen to the left
        }
    }

    fun resetGame() {
        resetRequested = true
    }

    LaunchedEffect(resetRequested) {
        if (resetRequested) {
            // Disable animation and instantly move train to right side
            animationEnabled = false
            trainPosition = 800f

            val newSequence = generateRandomSequence()
            currentSequence = newSequence
            selectedNumber = null
            coachNumbers = mutableMapOf(
                0 to null,
                1 to null,
                2 to newSequence[2],
                3 to null
            )
            availableNumbers = listOf(newSequence[0], newSequence[1], newSequence[3])
            gameCompleted = false
            showSteam = false

            // Wait a moment, then re-enable animation and move to center
            delay(100)
            animationEnabled = true
            delay(100)
            trainPosition = 0f // Now animate from right to center

            resetRequested = false
        }
    }
    val activity = LocalActivity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF87CEEB), // Sky blue
                        Color(0xFFE0F6FF), // Light blue
                        Color(0xFF98D8E8)  // Medium blue
                    )
                )
            )
    ) {
        // 🔙 Back Button
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
        // Clouds in the background
        CloudBackground()

        // Railway tracks background with more realistic design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50), // Green grass
                            Color(0xFF66BB6A),
                            Color(0xFF81C784)
                        )
                    )
                )
        ) {
            // Gravel bed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 20.dp)
                    .background(Color(0xFF8D6E63))
            )

            // Main railway tracks (two rails)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 28.dp)
                    .background(Color(0xFF37474F))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 38.dp)
                    .background(Color(0xFF37474F))
            )

            // Railway sleepers with more realistic spacing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset(y = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(12) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(28.dp)
                            .background(
                                Color(0xFF5D4037),
                                RoundedCornerShape(3.dp)
                            )
                            .shadow(2.dp, RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Enhanced title with gradient
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 8.dp),
//                colors = CardDefaults.cardColors(
//                    containerColor = Color.Transparent
//                ),
//                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(
//                            Brush.horizontalGradient(
//                                colors = listOf(
//                                    Color(0xFF1E88E5),
//                                    Color(0xFF1976D2),
//                                    Color(0xFF1565C0)
//                                )
//                            ),
//                            RoundedCornerShape(16.dp)
//                        )
//                        .padding(16.dp)
//                ) {
//                    Text(
//                        text = "🚂 Number Train Adventure",
//                        fontSize = 28.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.White,
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
//            }

            // Enhanced number options with better styling
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                availableNumbers.forEach { number ->
                    EnhancedNumberTile(
                        number = number,
                        isSelected = selectedNumber == number,
                        onClick = {
                            selectedNumber = if (selectedNumber == number) null else number
                        }
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (gameCompleted) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF4CAF50),
                                            Color(0xFF66BB6A),
                                            Color(0xFF81C784)
                                        )
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "🎉 Fantastic! All aboard! The train is departing! 🚂💨✨",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Enhanced realistic train with steam effect
                Box {
                    // Steam effect
                    if (showSteam) {
                        SteamEffect(
                            opacity = steamOpacity,
                            offset = animatedPosition.dp
                        )
                    }

                    // Enhanced train
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = animatedPosition.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Enhanced Train Engine
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EnhancedTrainEngine()
                            Spacer(modifier = Modifier.height(6.dp))
                            // Enhanced engine wheels
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.width(80.dp)
                            ) {
                                EnhancedWheel(size = 20.dp)
                                EnhancedWheel(size = 20.dp)
                                EnhancedWheel(size = 16.dp)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Enhanced Train Coaches
                        repeat(4) { index ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                EnhancedTrainCoach(
                                    number = coachNumbers[index],
                                    position = index,
                                    isFixed = index == 2,
                                    onClick = {
                                        if (selectedNumber != null && coachNumbers[index] == null && index != 2) {
                                            val correctNumber = currentSequence[index]
                                            if (selectedNumber == correctNumber) {
                                                coachNumbers = coachNumbers.toMutableMap().apply {
                                                    put(index, selectedNumber)
                                                }
                                                availableNumbers = availableNumbers.filter { it != selectedNumber }
                                                selectedNumber = null
                                            } else {
                                                selectedNumber = null
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                // Enhanced coach wheels
                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    EnhancedWheel(size = 14.dp)
                                    EnhancedWheel(size = 14.dp)
                                }
                            }

                            if (index < 3) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enhanced New Train Button
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF6B35),
                                    Color(0xFFFF8A50),
                                    Color(0xFFFFAB40)
                                )
                            ),
                            RoundedCornerShape(50.dp)
                        )
                        .shadow(8.dp, RoundedCornerShape(50.dp)),
                    shape = RoundedCornerShape(50.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        "🔄 New Adventure",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CloudBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Cloud 1
        Box(
            modifier = Modifier
                .size(80.dp, 40.dp)
                .offset(x = 50.dp, y = 60.dp)
                .background(
                    Color.White.copy(alpha = 0.8f),
                    RoundedCornerShape(20.dp)
                )
        )
        // Cloud 2
        Box(
            modifier = Modifier
                .size(100.dp, 50.dp)
                .offset(x = 250.dp, y = 40.dp)
                .background(
                    Color.White.copy(alpha = 0.7f),
                    RoundedCornerShape(25.dp)
                )
        )
        // Cloud 3
        Box(
            modifier = Modifier
                .size(60.dp, 30.dp)
                .offset(x = 150.dp, y = 80.dp)
                .background(
                    Color.White.copy(alpha = 0.6f),
                    RoundedCornerShape(15.dp)
                )
        )
    }
}

@Composable
fun SteamEffect(opacity: Float, offset: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .offset(x = offset + 32.dp, y = (-60).dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size((20 + index * 8).dp)
                    .offset(y = (index * (-15)).dp)
                    .background(
                        Color.White.copy(alpha = opacity * (0.8f - index * 0.2f)),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun EnhancedTrainEngine() {
    Box(
        modifier = Modifier.size(80.dp, 100.dp)
    ) {
        // Main engine body with gradient
        Card(
            modifier = Modifier
                .size(80.dp, 80.dp)
                .align(Alignment.BottomCenter),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE53935),
                                Color(0xFFD32F2F),
                                Color(0xFFB71C1C)
                            )
                        ),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
            ) {
                // Enhanced window
                Card(
                    modifier = Modifier
                        .size(40.dp, 30.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF81D4FA)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF81D4FA),
                                        Color(0xFF4FC3F7)
                                    )
                                )
                            )
                    )
                }

                // Enhanced front detail (cowcatcher)
                Card(
                    modifier = Modifier
                        .size(20.dp, 40.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = (-10).dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFC107)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {}

                // Door details
                Box(
                    modifier = Modifier
                        .size(16.dp, 24.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .background(
                            Color(0xFF424242),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        // Enhanced chimney with gradient
        Card(
            modifier = Modifier
                .size(16.dp, 32.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF424242),
                                Color(0xFF616161)
                            )
                        ),
                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                    )
            )
        }

        // Headlight
        Box(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-6).dp)
                .background(
                    Color(0xFFFFF9C4),
                    CircleShape
                )
                .border(2.dp, Color(0xFFFFD54F), CircleShape)
        )
    }
}

@Composable
fun EnhancedWheel(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier.size(size)
    ) {
        // Outer wheel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF616161),
                            Color(0xFF424242),
                            Color(0xFF212121)
                        )
                    ),
                    CircleShape
                )
                .border(1.dp, Color(0xFF757575), CircleShape)
        )
        // Inner hub
        Box(
            modifier = Modifier
                .size(size * 0.4f)
                .align(Alignment.Center)
                .background(Color(0xFF9E9E9E), CircleShape)
        )
    }
}

@Composable
fun EnhancedNumberTile(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(90.dp)
            .clickable { onClick() }
            .shadow(
                elevation = if (isSelected) 16.dp else 8.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSelected) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F),
                                Color(0xFFFFB300),
                                Color(0xFFFF8F00)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE3F2FD),
                                Color(0xFFBBDEFB),
                                Color(0xFF90CAF9)
                            )
                        )
                    },
                    RoundedCornerShape(16.dp)
                )
                .then(
                    if (isSelected)
                        Modifier.border(3.dp, Color(0xFFF57F17), RoundedCornerShape(16.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF1565C0)
            )
        }
    }
}

@Composable
fun EnhancedTrainCoach(
    number: Int?,
    position: Int,
    isFixed: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(92.dp, 92.dp)
            .clickable { if (!isFixed) onClick() }
            .shadow(6.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        isFixed -> Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF7043),
                                Color(0xFFFF5722),
                                Color(0xFFE64A19)
                            )
                        )
                        number != null -> Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF66BB6A),
                                Color(0xFF4CAF50),
                                Color(0xFF388E3C)
                            )
                        )
                        else -> Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE0E0E0),
                                Color(0xFFBDBDBD),
                                Color(0xFF9E9E9E)
                            )
                        )
                    },
                    RoundedCornerShape(12.dp)
                )
        ) {
            // Coach window
            Card(
                modifier = Modifier
                    .size(40.dp, 20.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF81D4FA)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {}

            // Door
            Box(
                modifier = Modifier
                    .size(12.dp, 20.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 6.dp, y = (-8).dp)
                    .background(
                        Color(0xFF424242),
                        RoundedCornerShape(3.dp)
                    )
            )

            // Number display
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number?.toString() ?: "?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
fun playTrainWhistleSound(context: Context) {
    val mediaPlayer = MediaPlayer.create(context, R.raw.trainwhistle)
    mediaPlayer.start()
    mediaPlayer.setOnCompletionListener { mp -> mp.release() }
}
