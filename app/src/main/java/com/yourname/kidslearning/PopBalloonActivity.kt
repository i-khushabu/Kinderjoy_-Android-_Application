package com.yourname.kidslearning

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.activity.compose.LocalActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp

class PopBalloonActivity : ComponentActivity() {
    private var sessionStartTime: Long = 0  // ✅ added

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)

        setContent {
            PopTheBalloonGame()
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartTime = System.currentTimeMillis()  // ✅ added
    }

    override fun onDestroy() {
        val sessionEndTime = System.currentTimeMillis()
        val durationMillis = sessionEndTime - sessionStartTime
        val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1)
        saveProgressToFirebase("Alphabets", durationMinutes)  // ✅ consistent

        AppSessionManager.cancelSession()
        super.onDestroy()
    }
}


@Composable
fun PopTheBalloonGame() {
    val configuration = LocalConfiguration.current
    val screenWidth = remember { configuration.screenWidthDp.dp }
    val screenHeight = remember { configuration.screenHeightDp.dp }

    var score by remember { mutableStateOf(0) }
    var targetLetter by remember { mutableStateOf(('A'..'Z').random()) }
    val balloons = remember { mutableStateListOf<Balloon>() }
    val popEffects = remember { mutableStateListOf<PopEffect>() }
    val context = LocalContext.current
    val popSound = remember { MediaPlayer.create(context, R.raw.correct) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(targetLetter) {
        balloons.clear()
        balloons.addAll(generateBalloons(screenWidth, targetLetter))
    }

    val bgImage = painterResource(id = R.drawable.bg_game)
    val activity = LocalActivity.current

    Box(modifier = Modifier.fillMaxSize()) {

        // Background image
        Image(
            painter = bgImage,
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
        // Centered Text Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Find the letter: $targetLetter",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Balloons
        balloons.forEach { balloon ->
            key(balloon.letter) {
                BalloonComposable(balloon, screenHeight) { poppedBalloon ->
                    if (poppedBalloon.letter == targetLetter) {
                        score++
                        popSound.start()
                        popEffects.add(PopEffect(poppedBalloon.x, screenHeight.value))

                        coroutineScope.launch {
                            delay(300)
                            targetLetter = ('A'..'Z').random()
                        }
                    }
                }
            }
        }

        // Sparkle pop effects
        popEffects.forEach { effect ->
            SparkleEffect(effect)
        }
    }
}

fun generateBalloons(screenWidth: Dp, targetLetter: Char): List<Balloon> {
    val numBalloons = 5
    val spacing = screenWidth.value / numBalloons
    val newBalloons = mutableListOf<Balloon>()

    // ✅ Random index where the target letter will appear
    val targetIndex = Random.nextInt(numBalloons)

    repeat(numBalloons) { index ->
        val letter = if (index == targetIndex) targetLetter else {
            var randomLetter: Char
            do {
                randomLetter = ('A'..'Z').random()
            } while (randomLetter == targetLetter)
            randomLetter
        }

        newBalloons.add(
            Balloon(
                letter = letter,
                x = (index * spacing),
                color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
            )
        )
    }

    return newBalloons
}

// Color Darkener
fun Color.darken(factor: Float): Color {
    return Color(
        red = (this.red * (1 - factor)).coerceIn(0f, 1f),
        green = (this.green * (1 - factor)).coerceIn(0f, 1f),
        blue = (this.blue * (1 - factor)).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}

// Composable for Balloons
@Composable
fun BalloonComposable(balloon: Balloon, screenHeight: Dp, onBalloonPopped: (Balloon) -> Unit) {
    val yOffset = remember { Animatable(screenHeight.value) }
    val xOffset = remember { Animatable(balloon.x) }
    var isPopping by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(balloon.letter) {
        coroutineScope.launch {
            while (true) {
                yOffset.animateTo(-100f, tween(durationMillis = 5000, easing = LinearEasing))
                yOffset.snapTo(screenHeight.value)
            }
        }
    }

    LaunchedEffect(balloon.letter) {
        coroutineScope.launch {
            while (true) {
                xOffset.animateTo(balloon.x + 10f, tween(1000, easing = LinearEasing))
                xOffset.animateTo(balloon.x - 10f, tween(1000, easing = LinearEasing))
            }
        }
    }

    val balloonSize by animateFloatAsState(
        targetValue = if (isPopping) 0f else 1f,
        animationSpec = tween(300)
    )

    if (balloonSize > 0f) {
        Box(
            modifier = Modifier
                .absoluteOffset(x = xOffset.value.dp, y = yOffset.value.dp)
                .size((100 * balloonSize).dp, (150 * balloonSize).dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        isPopping = true
                        onBalloonPopped(balloon)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                drawOval(
                    color = balloon.color,
                    topLeft = Offset(0f, 0f),
                    size = Size(width, height * 0.8f)
                )

                val knot = Path().apply {
                    moveTo(width / 2, height * 0.8f)
                    lineTo(width * 0.45f, height * 0.85f)
                    lineTo(width * 0.55f, height * 0.85f)
                    close()
                }
                drawPath(knot, balloon.color.darken(0.2f))

                val waveOffset = (xOffset.value - balloon.x) * 0.5f
                val start = Offset(width / 2, height * 0.85f)
                val end = Offset(width / 2 - waveOffset, height * 1.2f)

                drawLine(Color.Gray, start, end, strokeWidth = 3f)

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 50f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    canvas.nativeCanvas.drawText(
                        balloon.letter.toString(),
                        width / 2,
                        height / 2 + 20,
                        paint
                    )
                }
            }
        }
    }
}

// Balloon Data Class
data class Balloon(
    val letter: Char,
    var x: Float,
    val color: Color
)

// Sparkle Data Class
data class PopEffect(val x: Float, val y: Float)

// Sparkle Pop Animation
@Composable
fun SparkleEffect(effect: PopEffect) {
    val alpha = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            alpha.animateTo(0f, animationSpec = tween(500))
        }
    }

    Canvas(
        modifier = Modifier
            .absoluteOffset(x = effect.x.dp, y = effect.y.dp)
            .size(40.dp)
            .graphicsLayer(alpha = alpha.value)
    ) {
        drawCircle(Color.Yellow, radius = 20f)
        drawCircle(Color.White, radius = 10f)
    }
}
