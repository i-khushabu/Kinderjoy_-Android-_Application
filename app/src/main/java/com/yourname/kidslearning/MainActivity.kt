package com.yourname.kidslearning

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.compose.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        mediaPlayer = MediaPlayer.create(this, R.raw.theme).apply {
            isLooping = true
            start()
        }

        setContent {
            var screenState by remember { mutableStateOf("splash") } // "splash", "locked", "home", "exit"
            var lockUntilTime by remember { mutableStateOf(0L) }

            LaunchedEffect(Unit) {
                delay(6000) // Splash time

                try {
                    val ref = FirebaseDatabase.getInstance("https://kinderjoy-372c6-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .getReference("ScreenLock/lockUntil")

                    val snapshot = ref.get().await()
                    val lockUntil = snapshot.getValue(Long::class.java) ?: 0L
                    val currentTime = System.currentTimeMillis()

                    if (currentTime < lockUntil) {
                        lockUntilTime = lockUntil
                        screenState = "locked"
                    } else {
                        val timeRef = FirebaseDatabase.getInstance("https://kinderjoy-372c6-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("parentalSettings/screenTimeLimit")

                        val timeSnap = timeRef.get().await()
                        val hours = timeSnap.child("hours").getValue(Int::class.java) ?: 0
                        val minutes = timeSnap.child("minutes").getValue(Int::class.java) ?: 0
                        val totalMinutes = hours * 60 + minutes
                        if (totalMinutes > 0) {
                            AppSessionManager.init(this@MainActivity, totalMinutes)
                        }
                        screenState = "home"


                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    screenState = "home"
                }
            }


            when (screenState) {
                "splash" -> SplashScreen()
                "locked" -> LockedScreen(lockUntilTime)
                "exit" -> ExitScreen { finishAffinity() }
                "home" -> HomeScreen { gameType ->
                    if (gameType == "EXIT") screenState = "exit"
                    else navigateToGameScreen(gameType)
                }
            }
        }

    }


    private fun navigateToGameScreen(gameType: String) {
        mediaPlayer?.pause()
        val intent = when (gameType) {
            "Numbers" -> Intent(this, MathPuzzleActivity::class.java)
            "Alphabets" -> Intent(this, ABCLearningActivity::class.java)
            "Shapes" -> Intent(this, ShapeRecognitionActivity::class.java)
            "Coloring" -> Intent(this, ColoringGameActivity::class.java)
            "Mythology" -> Intent(this, MythologySectionActivity::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }
    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.theme).apply {
                isLooping = true
                start()
            }
        } else if (!mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null

    }
}

// ================= UI COMPOSABLES ================= //


@Composable
fun HomeScreen(navigateToGameScreen: (String) -> Unit) {
    var selectedGame by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedGame) {
        if (selectedGame != null) {
            delay(1500)
            val gameToLaunch = selectedGame!!
            selectedGame = null
            navigateToGameScreen(gameToLaunch)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.backs),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .align(Alignment.TopStart)
        ) {
            Image(
                painter = painterResource(id = R.drawable.kidslogo),
                contentDescription = "App Logo",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("KinderJoy", fontSize = 20.sp, color = Color.White)
                Text("The Joyful Learning", fontSize = 16.sp, color = Color.White)
            }
        }

        val context = LocalContext.current
        Row(
            modifier = Modifier
                .padding(top = 16.dp, end = 16.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { context.startActivity(Intent(context, ParentalDashboardActivity::class.java)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Text("Parents", fontSize = 14.sp)
            }

            Button(
                onClick = { selectedGame = "EXIT" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Text("Exit", fontSize = 14.sp)
            }
        }

        if (selectedGame != null) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ani2))
            val progress by animateLottieCompositionAsState(composition)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(200.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 110.dp, start = 76.dp, end = 76.dp),
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
                    Spacer(modifier = Modifier.width(20.dp))
                    GameButton(R.drawable.math, "Numbers") { selectedGame = "Numbers" }
                    GameButton(R.drawable.abc, "Alphabets") { selectedGame = "Alphabets" }
                    GameButton(R.drawable.shape, "Shapes") { selectedGame = "Shapes" }
                    GameButton(R.drawable.color, "Coloring") { selectedGame = "Coloring" }
                    GameButton(R.drawable.myth, "Mythology") { selectedGame = "Mythology" }
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }
    }
}

@Composable
fun ExitScreen(onExitDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onExitDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFE082)),
        contentAlignment = Alignment.Center
    ) {
        Text("Good Bye!", fontSize = 40.sp, color = Color.Black)
    }
}

@Composable
fun SplashScreen() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "fadeIn"
    )
    LaunchedEffect(Unit) {
        visible = true
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Cyan),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(500.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
        )
    }
}

@Composable
fun GameButton(imageResId: Int, label: String, onClick: () -> Unit) {
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
            .width(240.dp)
            .height(220.dp)
            .padding(2.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(label, fontSize = 22.sp, color = Color.Black, modifier = Modifier.padding(top = 4.dp))
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
@Composable
fun LockedScreen(lockUntil: Long) {
    val context = LocalContext.current
    val currentTime = remember { System.currentTimeMillis() }
    val timeLeft = lockUntil - currentTime
    val hours = (timeLeft / (1000 * 60 * 60)).coerceAtLeast(0)
    val minutes = ((timeLeft / (1000 * 60)) % 60).coerceAtLeast(0)

    var showPuzzleDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEBEE)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⛔ Screen Time Over", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Come back in $hours hours $minutes minutes", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { showPuzzleDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("Solve Puzzle to Unlock")
            }
        }
    }

    if (showPuzzleDialog) {
        UnlockPuzzleDialog(
            onDismiss = { showPuzzleDialog = false },
            onSolved = {
                val db = FirebaseDatabase.getInstance("https://kinderjoy-372c6-default-rtdb.asia-southeast1.firebasedatabase.app")

                // ✅ 1. Remove Lock
                db.getReference("ScreenLock/lockUntil").setValue(0L)

                // ✅ 2. Clear old time limit
                db.getReference("parentalSettings/screenTimeLimit").setValue(
                    mapOf("hours" to 0, "minutes" to 0)
                )

                // ✅ 3. Stop countdown timer if it was still running
                AppSessionManager.cancelSession()

                // ✅ 4. Reload the activity to go to home screen
                val activity = context as? Activity
                activity?.recreate()
            }

        )
    }
}
@Composable
fun UnlockPuzzleDialog(
    onDismiss: () -> Unit,
    onSolved: () -> Unit
) {
    val num1 = remember { (1..10).random() }
    val num2 = remember { (1..10).random() }
    val correctAnswer = num1 + num2
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parental Puzzle") },
        text = {
            Column {
                Text("What is $num1 + $num2 ?")
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Your Answer") },
                    singleLine = true
                )
                if (error.isNotEmpty()) {
                    Text(error, color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (input.toIntOrNull() == correctAnswer) {
                    onSolved()
                } else {
                    error = "Incorrect answer! Try again."
                }
            }) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}





@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    HomeScreen {}
}
