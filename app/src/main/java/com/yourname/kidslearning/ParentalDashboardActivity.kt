package com.yourname.kidslearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import android.app.Activity


class ParentalDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)
        setContent {
            ParentalDashboardScreen()
        }
    }
}

@Composable
fun ParentalDashboardScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8F4EF)
    ) {
        ParentalDashboard()
    }
}


@Composable
fun ParentalDashboard() {
    var screenLimit by remember { mutableStateOf("Loading...") }
    var showDialog by remember { mutableStateOf(false) }

    // Fetch time limit from Firebase
    LaunchedEffect(Unit) {
        fetchTimeLimitFromFirebase { hours, minutes ->
            screenLimit = "${hours}h ${minutes}m"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F4EF)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val activity = LocalActivity.current

        // 🔵 Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF7E6AD1))
                .padding(16.dp)
        ) {
            // 🟣 Back IconButton (top-left)
            IconButton(
                onClick = { activity?.finish() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "PARENTAL DASHBOARD",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🧸 Mascot Animation (Lottie or Image Placeholder)
        // Replace with your Lottie file
        /*
        LottieAnimation(
            composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.mascot_animation)).value,
            modifier = Modifier.size(120.dp)
        )
        */

        Spacer(modifier = Modifier.height(16.dp))

        // 🟠 Dashboard Cards in Center Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardCard(
                title = "SCREEN LIMIT",
                icon = Icons.Default.AccessTime,
                content = screenLimit,
                buttonText = "SET",
                color = Color(0xFFF8972B),
                onClick = { showDialog = true }
            )

            val context = LocalContext.current
            DashboardCard(
                title = "TRACK PROGRESS",
                icon = Icons.Default.BarChart,
                content = "Updated Today",
                buttonText = "VIEW DETAILS",
                color = Color(0xFF3CBAC8),
                onClick = {
                    context.startActivity(Intent(context, TrackProgressActivity::class.java))
                }
            )
        }

        // ⏱️ Set Time Limit Dialog
        if (showDialog) {
            SetTimeLimitDialog(
                onDismiss = { showDialog = false },
                onTimeSaved = { screenLimit = it }
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    content: String,
    buttonText: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(230.dp)
            .height(220.dp) // ✅ Fixed height to match cards
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // ✅ Distribute space equally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(58.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (content.isNotEmpty()) {
                    Text(
                        content,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp)) // keeps balance
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .height(36.dp)
                    .width(180.dp)
            ) {
                Text(buttonText, color = Color.White, maxLines = 1, softWrap = false)
            }
        }
    }
}


@Composable
fun SetTimeLimitDialog(
    onDismiss: () -> Unit,
    onTimeSaved: (String) -> Unit
) {
    val context = LocalContext.current
    var hours by remember { mutableStateOf("1") }
    var minutes by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Screen Time Limit") },
        text = {
            Column {
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Hours") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = { Text("Minutes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val h = hours.toIntOrNull() ?: 0
                val m = minutes.toIntOrNull() ?: 0

                saveTimeLimitToFirebase(h, m)

                val formatted = "${h}h ${m}m"
                onTimeSaved(formatted)
                onDismiss()

                // ✅ Start session timer correctly using AppSessionManager
                AppSessionManager.init(context, h * 60 + m)

            }) {
                Text("Save")
            }
        }
        ,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
