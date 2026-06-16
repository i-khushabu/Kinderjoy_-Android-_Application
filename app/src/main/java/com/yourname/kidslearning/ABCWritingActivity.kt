package com.yourname.kidslearning

import android.graphics.PathMeasure
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

import kotlinx.coroutines.delay
import kotlin.math.sqrt


class ABCWritingActivity : ComponentActivity() {
    private var sessionStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)
        setContent {
            ABCWritingScreen()
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
        saveProgressToFirebase("Alphabets", durationMinutes)

        super.onDestroy()
        AppSessionManager.cancelSession()
    }

}



data class TracingPathPoint(val x: Float, val y: Float, val isInLetter: Boolean = false)

data class LetterStroke(val path: Path, val isCompleted: Boolean = false)

@Composable
fun ABCWritingScreen() {
    val letters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
    var currentLetterIndex by remember { mutableIntStateOf(0) }
    val currentLetter = letters[currentLetterIndex]

    var showDottedGuide by remember { mutableStateOf(true) }
    var completionPercentage by remember { mutableFloatStateOf(0f) }
    val progressAlpha by animateFloatAsState(targetValue = completionPercentage, label = "progress")

    var userPaths by remember { mutableStateOf(listOf<List<TracingPathPoint>>()) }
    var currentPath by remember { mutableStateOf(listOf<TracingPathPoint>()) }

    // Store the letter path to use for hit detection
    val letterPathBounds = remember { mutableStateOf<List<Path>>(listOf()) }

    // Recognition states
    var showSuccessMessage by remember { mutableStateOf(false) }
    var isLetterCompleted by remember { mutableStateOf(false) }
    var strokeCompletionStatus by remember { mutableStateOf<List<Boolean>>(listOf()) }

    // Colors
    val backgroundColor = Color(0xFF87CEEB)
    val boardColor = Color.White
    val pathColor = Color(0xFFFF69B4)
    val letterOutlineColor = Color(0xFFFF69B4)
    val letterFillColor = Color(0xFF32CD32)
    val successColor = Color(0xFF4CAF50)

    // Check if we're in landscape mode
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Auto-progress to next letter when completed
    LaunchedEffect(isLetterCompleted) {
        if (isLetterCompleted && currentLetterIndex < letters.size - 1) {
            delay(2000) // Show success message for 2 seconds
            currentLetterIndex++
            userPaths = listOf()
            currentPath = listOf()
            completionPercentage = 0f
            showSuccessMessage = false
            isLetterCompleted = false
            strokeCompletionStatus = listOf()
        }
    }
    val context = LocalContext.current // add this inside the Composable function
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {


        // Back Button on top
        Box(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .zIndex(1f) // 👈 bring above other content
        ) {
            Text(
                text = "Back",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0xFF6200EE), shape = RoundedCornerShape(12.dp))
                    .clickable {
                        (context as? ComponentActivity)?.finish()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {


            // Drawing board - reduced in size for landscape mode
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(if (isLandscape) 0.4f else 0.8f)
                    .fillMaxHeight(if (isLandscape) 0.8f else 0.6f),
                colors = CardDefaults.cardColors(containerColor = boardColor)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Draw letter and store the path for hit detection
                        letterPathBounds.value = drawLetterPath(
                            currentLetter,
                            letterOutlineColor,
                            letterFillColor,
                            showDottedGuide,
                            strokeCompletionStatus
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val isInLetter = isPointInLetter(letterPathBounds.value, offset, IntSize(size.width.toInt(), size.height.toInt()))
                                        currentPath = listOf(
                                            TracingPathPoint(
                                                offset.x,
                                                offset.y,
                                                isInLetter
                                            )
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        val isInLetter = isPointInLetter(letterPathBounds.value, change.position, IntSize(size.width.toInt(), size.height.toInt()))
                                        val newPoint = TracingPathPoint(
                                            change.position.x,
                                            change.position.y,
                                            isInLetter
                                        )
                                        currentPath = currentPath + newPoint
                                        change.consume()
                                    },
                                    onDragEnd = {
                                        userPaths = userPaths + listOf(currentPath)

                                        // Check stroke completion and overall letter completion
                                        val (newStrokeStatus, newCompletionPercentage) = checkLetterCompletion(
                                            userPaths,
                                            letterPathBounds.value,
                                            currentLetter
                                        )

                                        strokeCompletionStatus = newStrokeStatus
                                        completionPercentage = newCompletionPercentage

                                        // Check if letter is completed (all strokes traced)
                                        if (newCompletionPercentage >= 0.9f && !isLetterCompleted) {
                                            isLetterCompleted = true
                                            showSuccessMessage = true
                                        }
                                    }
                                )
                            }
                    ) {
                        // Draw only the points that are within the letter
                        userPaths.forEach { points ->
                            // Split the path into segments based on isInLetter
                            val segments = mutableListOf<List<TracingPathPoint>>()
                            var currentSegment = mutableListOf<TracingPathPoint>()

                            for (i in points.indices) {
                                val point = points[i]
                                if (point.isInLetter) {
                                    currentSegment.add(point)
                                } else if (currentSegment.isNotEmpty()) {
                                    segments.add(currentSegment)
                                    currentSegment = mutableListOf()
                                }
                            }

                            // Add the last segment if it's not empty
                            if (currentSegment.isNotEmpty()) {
                                segments.add(currentSegment)
                            }

                            // Draw each segment that's inside the letter
                            segments.forEach { segment ->
                                if (segment.isNotEmpty()) {
                                    val path = Path()
                                    path.moveTo(segment.first().x, segment.first().y)
                                    segment.forEach { point ->
                                        path.lineTo(point.x, point.y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = pathColor,
                                        style = Stroke(
                                            width = 58f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }

                        // Draw current path - only the points within the letter
                        if (currentPath.isNotEmpty()) {
                            // Similar logic for current path
                            val segments = mutableListOf<List<TracingPathPoint>>()
                            var currentSegment = mutableListOf<TracingPathPoint>()

                            for (i in currentPath.indices) {
                                val point = currentPath[i]
                                if (point.isInLetter) {
                                    currentSegment.add(point)
                                } else if (currentSegment.isNotEmpty()) {
                                    segments.add(currentSegment)
                                    currentSegment = mutableListOf()
                                }
                            }

                            if (currentSegment.isNotEmpty()) {
                                segments.add(currentSegment)
                            }

                            segments.forEach { segment ->
                                if (segment.isNotEmpty()) {
                                    val path = Path()
                                    path.moveTo(segment.first().x, segment.first().y)
                                    segment.forEach { point ->
                                        path.lineTo(point.x, point.y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = pathColor,
                                        style = Stroke(
                                            width = 58f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progressAlpha },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .align(Alignment.BottomCenter)
                            .alpha(0.7f),
                        color = Color(0xFF4CAF50),
                        trackColor = Color.LightGray
                    )

                    // Success message overlay
                    if (showSuccessMessage) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            colors = CardDefaults.cardColors(containerColor = successColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎉 Well Done! 🎉",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )


                            }
                        }
                    }
                }
            }

        }

        // Navigation buttons - positioned on the right side for landscape mode
        Column(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.medium),
                onClick = {
                    if (currentLetterIndex > 0) {
                        currentLetterIndex--
                        userPaths = listOf()
                        currentPath = listOf()
                        completionPercentage = 0f
                        showSuccessMessage = false
                        isLetterCompleted = false
                        strokeCompletionStatus = listOf()
                    }
                }
            ) {
                Text("←", fontSize = 30.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            IconButton(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.medium),
                onClick = {
                    if (currentLetterIndex < letters.size - 1) {
                        currentLetterIndex++
                        userPaths = listOf()
                        currentPath = listOf()
                        completionPercentage = 0f
                        showSuccessMessage = false
                        isLetterCompleted = false
                        strokeCompletionStatus = listOf()
                    }
                }
            ) {
                Text("→", fontSize = 30.sp, color = Color.White)
            }

            // Clear button
            Spacer(modifier = Modifier.height(32.dp))
            IconButton(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.medium),
                onClick = {
                    userPaths = listOf()
                    currentPath = listOf()
                    completionPercentage = 0f
                    showSuccessMessage = false
                    isLetterCompleted = false
                    strokeCompletionStatus = listOf()
                }
            ) {
                Text("⟲", fontSize = 30.sp, color = Color.White)
            }
        }
    }
}}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLetterPath(
    letter: String,
    outlineColor: Color,
    fillColor: Color,
    showDottedGuide: Boolean,
    strokeCompletionStatus: List<Boolean>
): List<Path> {
    val strokePaths = mutableListOf<Path>()

    when (letter) {
        "A" -> {
            val centerX = size.width / 2
            val topPointY = size.height * 0.15f
            val leftBottomX = centerX - size.width * 0.35f
            val rightBottomX = centerX + size.width * 0.35f
            val bottomY = size.height * 0.85f
            val crossbarY = size.height * 0.5f

            // Left diagonal stroke
            val leftDiagonal = Path().apply {
                moveTo(centerX, topPointY)
                lineTo(leftBottomX, bottomY)
            }
            strokePaths.add(leftDiagonal)

            // Right diagonal stroke
            val rightDiagonal = Path().apply {
                moveTo(centerX, topPointY)
                lineTo(rightBottomX, bottomY)
            }
            strokePaths.add(rightDiagonal)

            // Crossbar stroke
            val crossbar = Path().apply {
                moveTo(leftBottomX + size.width * 0.15f, crossbarY)
                lineTo(rightBottomX - size.width * 0.15f, crossbarY)
            }
            strokePaths.add(crossbar)

            // Draw each stroke with appropriate color based on completion
            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(15f, 15f), 0f
                            )
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "B" -> {
            val leftX = size.width * 0.2f
            val topY = size.height * 0.15f
            val bottomY = size.height * 0.85f
            val rightCurveX = size.width * 0.8f
            val middleY = size.height * 0.5f

            // Vertical stroke
            val verticalStroke = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(verticalStroke)

            // Top curve
            val topCurve = Path().apply {
                moveTo(leftX, topY)
                lineTo(rightCurveX - size.width * 0.2f, topY)
                cubicTo(
                    rightCurveX, topY,
                    rightCurveX, middleY - size.height * 0.1f,
                    leftX, middleY
                )
            }
            strokePaths.add(topCurve)

            // Bottom curve
            val bottomCurve = Path().apply {
                moveTo(leftX, middleY)
                lineTo(rightCurveX - size.width * 0.2f, middleY)
                cubicTo(
                    rightCurveX, middleY,
                    rightCurveX, bottomY,
                    leftX, bottomY
                )
            }
            strokePaths.add(bottomCurve)

            // Draw each stroke with appropriate color based on completion
            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(15f, 15f), 0f
                            )
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "C" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val centerY = size.height * 0.5f

            val cPath = Path().apply {
                moveTo(rightX, topY)
                cubicTo(leftX, topY, leftX, bottomY, rightX, bottomY)
            }
            strokePaths.add(cPath)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "D" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val centerY = size.height * 0.5f

            val verticalLine = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(verticalLine)

            val curve = Path().apply {
                moveTo(leftX, topY)
                cubicTo(rightX, topY, rightX, bottomY, leftX, bottomY)
            }
            strokePaths.add(curve)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "E" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val middleY = size.height * 0.5f

            val verticalLine = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(verticalLine)

            val topLine = Path().apply {
                moveTo(leftX, topY)
                lineTo(rightX, topY)
            }
            strokePaths.add(topLine)

            val middleLine = Path().apply {
                moveTo(leftX, middleY)
                lineTo(leftX + (rightX - leftX) * 0.6f, middleY)
            }
            strokePaths.add(middleLine)

            val bottomLine = Path().apply {
                moveTo(leftX, bottomY)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(bottomLine)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "F" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val middleY = size.height * 0.5f

            // Vertical line
            val verticalLine = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(verticalLine)

            // Top horizontal line
            val topLine = Path().apply {
                moveTo(leftX, topY)
                lineTo(rightX, topY)
            }
            strokePaths.add(topLine)

            // Middle horizontal line (shorter than top)
            val middleLine = Path().apply {
                moveTo(leftX, middleY)
                lineTo(leftX + (rightX - leftX) * 0.7f, middleY)
            }
            strokePaths.add(middleLine)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "G" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val centerY = size.height * 0.5f
            val centerX = size.width * 0.5f

            // Main C-shaped curve
            val mainCurve = Path().apply {
                moveTo(rightX, topY)
                cubicTo(leftX, topY, leftX, bottomY, rightX, bottomY)
            }
            strokePaths.add(mainCurve)

            // Horizontal line in the middle (from center to right)
            val horizontalLine = Path().apply {
                moveTo(centerX, centerY)
                lineTo(rightX, centerY)
            }
            strokePaths.add(horizontalLine)

            // Small vertical line at the right (connecting horizontal line to bottom curve)
            val verticalLine = Path().apply {
                moveTo(rightX, centerY)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(verticalLine)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "H" -> {
            val leftX = size.width * 0.25f
            val rightX = size.width * 0.75f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val middleY = size.height * 0.5f

            // Left vertical line
            val leftVertical = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(leftVertical)

            // Right vertical line
            val rightVertical = Path().apply {
                moveTo(rightX, topY)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(rightVertical)

            // Horizontal crossbar
            val crossbar = Path().apply {
                moveTo(leftX, middleY)
                lineTo(rightX, middleY)
            }
            strokePaths.add(crossbar)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "I" -> {
            val centerX = size.width * 0.5f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val lineWidth = size.width * 0.3f

            // Top horizontal line
            val topLine = Path().apply {
                moveTo(centerX - lineWidth / 2, topY)
                lineTo(centerX + lineWidth / 2, topY)
            }
            strokePaths.add(topLine)

            // Vertical line
            val verticalLine = Path().apply {
                moveTo(centerX, topY)
                lineTo(centerX, bottomY)
            }
            strokePaths.add(verticalLine)

            // Bottom horizontal line
            val bottomLine = Path().apply {
                moveTo(centerX - lineWidth / 2, bottomY)
                lineTo(centerX + lineWidth / 2, bottomY)
            }
            strokePaths.add(bottomLine)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "J" -> {
            val rightX = size.width * 0.7f
            val leftX = size.width * 0.3f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val curveY = size.height * 0.7f

            // Top horizontal line
            val topLine = Path().apply {
                moveTo(leftX, topY)
                lineTo(rightX, topY)
            }
            strokePaths.add(topLine)

            // Vertical line with curve at bottom
            val verticalCurve = Path().apply {
                moveTo(rightX, topY)
                lineTo(rightX, curveY)
                cubicTo(rightX, bottomY, leftX, bottomY, leftX, curveY)
            }
            strokePaths.add(verticalCurve)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "K" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val middleY = size.height * 0.5f

            // Vertical line
            val verticalLine = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(verticalLine)

            // Upper diagonal line
            val upperDiagonal = Path().apply {
                moveTo(rightX, topY)
                lineTo(leftX, middleY)
            }
            strokePaths.add(upperDiagonal)

            // Lower diagonal line
            val lowerDiagonal = Path().apply {
                moveTo(leftX, middleY)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(lowerDiagonal)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "L" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f

            // Vertical line
            val verticalLine = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(verticalLine)

            // Bottom horizontal line
            val bottomLine = Path().apply {
                moveTo(leftX, bottomY)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(bottomLine)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "M" -> {
            val leftX = size.width * 0.15f
            val rightX = size.width * 0.85f
            val centerX = size.width * 0.5f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f
            val peakY = size.height * 0.4f

            // Left vertical line
            val leftVertical = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(leftVertical)

            // Left diagonal to center peak
            val leftDiagonal = Path().apply {
                moveTo(leftX, topY)
                lineTo(centerX, peakY)
            }
            strokePaths.add(leftDiagonal)

            // Right diagonal from center peak
            val rightDiagonal = Path().apply {
                moveTo(centerX, peakY)
                lineTo(rightX, topY)
            }
            strokePaths.add(rightDiagonal)

            // Right vertical line
            val rightVertical = Path().apply {
                moveTo(rightX, topY)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(rightVertical)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "N" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.8f

            // Left vertical line
            val leftVertical = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, bottomY)
            }
            strokePaths.add(leftVertical)

            // Diagonal line from bottom-left to top-right
            val diagonal = Path().apply {
                moveTo(leftX, bottomY)
                lineTo(rightX, topY)
            }
            strokePaths.add(diagonal)

            // Right vertical line
            val rightVertical = Path().apply {
                moveTo(rightX, topY)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(rightVertical)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }
        "O" -> {
            val centerX = size.width * 0.5f
            val centerY = size.height * 0.5f
            val radiusX = size.width * 0.3f
            val radiusY = size.height * 0.3f

            // Oval/Circle path
            val ovalPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        centerX - radiusX,
                        centerY - radiusY,
                        centerX + radiusX,
                        centerY + radiusY
                    )
                )
            }
            strokePaths.add(ovalPath)

            strokePaths.forEachIndexed { index, path ->
                val isCompleted = strokeCompletionStatus.getOrNull(index) ?: false
                val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
                val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = strokeOutlineColor,
                    style = if (showDottedGuide && !isCompleted) {
                        Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                )
            }
        }


        // Add more letters as needed
        else -> {
            val path = Path().apply {
                val leftX = size.width * 0.25f
                val rightX = size.width * 0.75f
                val topY = size.height * 0.15f
                val bottomY = size.height * 0.85f

                moveTo(leftX, topY)
                lineTo(rightX, topY)
                lineTo(rightX, bottomY)
                lineTo(leftX, bottomY)
                close()
            }

            strokePaths.add(path)

            val isCompleted = strokeCompletionStatus.getOrNull(0) ?: false
            val strokeColor = if (isCompleted) Color(0xFF4CAF50) else fillColor
            val strokeOutlineColor = if (isCompleted) Color(0xFF388E3C) else outlineColor

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 60f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawPath(
                path = path,
                color = strokeOutlineColor,
                style = if (showDottedGuide && !isCompleted) {
                    Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(15f, 15f), 0f
                        )
                    )
                } else {
                    Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                }
            )
        }
    }

    return strokePaths
}

private fun isPointInLetter(letterPaths: List<Path>, point: Offset, size: IntSize): Boolean {
    val strokeWidth = 60f // Match the stroke width used for drawing

    // Check if the point is within any of the letter's strokes
    for (composePath in letterPaths) {
        val androidPath = composePath.asAndroidPath() // Conversion to Android Path
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(androidPath, false)
        val pathLength = pathMeasure.length
        var distance = 0f
        val pos = FloatArray(2)

        while (distance < pathLength) {
            pathMeasure.getPosTan(distance, pos, null)
            val pathPoint = Offset(pos[0], pos[1])
            val distanceToPoint = distanceBetween(pathPoint, point)

            if (distanceToPoint <= strokeWidth / 2) {
                return true
            }
            distance += 5f
        }
    }
    return false
}

// Helper function to calculate distance between two points
private fun distanceBetween(point1: Offset, point2: Offset): Float {
    val dx = point1.x - point2.x
    val dy = point1.y - point2.y
    return sqrt(dx * dx + dy * dy)
}

private fun checkLetterCompletion(
    paths: List<List<TracingPathPoint>>,
    letterPaths: List<Path>,
    letter: String
): Pair<List<Boolean>, Float> {
    if (letterPaths.isEmpty()) return Pair(listOf(), 0f)

    val strokeCompletionStatus = mutableListOf<Boolean>()
    var totalCompletionScore = 0f

    // Check each stroke of the letter
    for (strokeIndex in letterPaths.indices) {
        val strokePath = letterPaths[strokeIndex]
        val androidPath = strokePath.asAndroidPath()
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(androidPath, false)
        val pathLength = pathMeasure.length

        if (pathLength == 0f) {
            strokeCompletionStatus.add(false)
            continue
        }

        // Sample points along the stroke path
        val samplePoints = mutableListOf<Offset>()
        val sampleInterval = 10f
        var distance = 0f
        val pos = FloatArray(2)

        while (distance < pathLength) {
            pathMeasure.getPosTan(distance, pos, null)
            samplePoints.add(Offset(pos[0], pos[1]))
            distance += sampleInterval
        }

        // Check how many sample points have been traced
        var tracedPoints = 0
        val tolerance = 40f // How close the user's trace needs to be

        for (samplePoint in samplePoints) {
            var isPointTraced = false

            // Check all user paths to see if any point is close to this sample point
            for (userPath in paths) {
                for (userPoint in userPath) {
                    if (userPoint.isInLetter) {
                        val distance = distanceBetween(
                            Offset(userPoint.x, userPoint.y),
                            samplePoint
                        )
                        if (distance <= tolerance) {
                            isPointTraced = true
                            break
                        }
                    }
                }
                if (isPointTraced) break
            }

            if (isPointTraced) tracedPoints++
        }

        // Calculate completion percentage for this stroke
        val strokeCompletion = if (samplePoints.isNotEmpty()) {
            tracedPoints.toFloat() / samplePoints.size
        } else {
            0f
        }

        // Consider stroke completed if 70% or more is traced
        val isStrokeCompleted = strokeCompletion >= 0.8f
        strokeCompletionStatus.add(isStrokeCompleted)

        totalCompletionScore += strokeCompletion
    }

    // Calculate overall completion percentage
    val overallCompletion = if (letterPaths.isNotEmpty()) {
        totalCompletionScore / letterPaths.size
    } else {
        0f
    }

    return Pair(strokeCompletionStatus, overallCompletion)
}

@Preview
@Composable
fun ABCWritingScreenPreview() {
    ABCWritingScreen()
}