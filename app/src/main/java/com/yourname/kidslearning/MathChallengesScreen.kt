package com.yourname.kidslearning

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp

@Composable
fun MathChallengesScreen(onChallengeSelected: (String) -> Unit) {
    val activity = LocalActivity.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.backs),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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

        // Centered Row with 2 square cards
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameCard(
                title = "Match The Pair",
                imageRes = R.drawable.matchthepair,
                onClick = { onChallengeSelected("Match The Pair") }
            )

            Spacer(modifier = Modifier.width(16.dp))

            GameCard(
                title = "Train Game",
                imageRes = R.drawable.traingame,
                onClick = { onChallengeSelected("Train Game") }
            )
        }
    }
}


@Composable
fun GameCard(title: String, imageRes: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(280.dp)
            .padding(8.dp),
        color = Color(0xFF6C4AB6),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.size(200.dp)
            )

        }
    }
}

@Preview
@Composable
fun PreviewMathChallengesScreen() {
    MathChallengesScreen(onChallengeSelected = {})
}
