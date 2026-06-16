package com.yourname.kidslearning

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource


import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

class MythologySectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)



        setContent {
            MythologySectionScreen()
        }
    }

    override fun onDestroy() {
        AppSessionManager.cancelSession()
        super.onDestroy()

    }
}



@Composable
fun MythologySectionScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {

        // ✅ Back Button on top-right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(1f),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = "Back",
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = Color(0xFF6200EE),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        (context as? ComponentActivity)?.finish()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ✅ Scrollable Row below back button
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 46.dp, end = 46.dp, bottom = 8.dp)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            MythologyCard(R.drawable.mahabharata) {
                context.startActivity(Intent(context, MahabharataActivity::class.java))
            }
            MythologyCard(R.drawable.ramayan) {
                context.startActivity(Intent(context, RamayanaActivity::class.java))
            }
            MythologyCard(R.drawable.krishna) {
                context.startActivity(Intent(context, KrishnaSectionActivity::class.java))
            }
            MythologyCard(R.drawable.ganesha) {
                context.startActivity(Intent(context, GaneshaSectionActivity::class.java))
            }
            MythologyCard(R.drawable.myth) {
                context.startActivity(Intent(context, MythologyQuizActivity::class.java))
            }
        }
    }
}

@Composable
fun MythologyCard(imageResId: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(220.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMythologySectionScreen() {
    MythologySectionScreen()
}
