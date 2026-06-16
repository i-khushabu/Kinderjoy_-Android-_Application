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

class MathWritingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)
        setContent {
            NumberWritingScreen()
        }
    }
}

data class TracingPathPoints(val x: Float, val y: Float, val isInNumber: Boolean = false)

data class NumberStroke(val path: Path, val isCompleted: Boolean = false)

@Composable
fun NumberWritingScreen() {
    val numbers = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
    var currentNumberIndex by remember { mutableIntStateOf(0) }
    val currentNumber = numbers[currentNumberIndex]

    var showDottedGuide by remember { mutableStateOf(true) }
    var completionPercentage by remember { mutableFloatStateOf(0f) }
    val progressAlpha by animateFloatAsState(targetValue = completionPercentage, label = "progress")

    var userPaths by remember { mutableStateOf(listOf<List<TracingPathPoints>>()) }
    var currentPaths by remember { mutableStateOf(listOf<TracingPathPoints>()) }

    // Store the number path to use for hit detection
    val numberPathBounds = remember { mutableStateOf<List<Path>>(listOf()) }

    // Recognition states
    var showSuccessMessage by remember { mutableStateOf(false) }
    var isNumberCompleted by remember { mutableStateOf(false) }
    var strokeCompletionStatus by remember { mutableStateOf<List<Boolean>>(listOf()) }

    // Colors
    val backgroundColor = Color(0xFF87CEEB)
    val boardColor = Color.White
    val pathColor = Color(0xFFFF69B4)
    val numberOutlineColor = Color(0xFFFF69B4)
    val numberFillColor = Color(0xFF32CD32)
    val successColor = Color(0xFF4CAF50)

    // Check if we're in landscape mode
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Auto-progress to next number when completed
    LaunchedEffect(isNumberCompleted) {
        if (isNumberCompleted && currentNumberIndex < numbers.size - 1) {
            delay(2000) // Show success message for 2 seconds
            currentNumberIndex++
            userPaths = listOf()
            currentPaths = listOf()
            completionPercentage = 0f
            showSuccessMessage = false
            isNumberCompleted = false
            strokeCompletionStatus = listOf()
        }
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Back Button on top
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
                // Drawing board
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
                            // Draw number and store the path for hit detection
                            numberPathBounds.value = drawNumberPath(
                                currentNumber,
                                numberOutlineColor,
                                numberFillColor,
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
                                            val isInNumber = isPointInNumber(numberPathBounds.value, offset, IntSize(size.width.toInt(), size.height.toInt()))
                                            currentPaths = listOf(
                                                TracingPathPoints(
                                                    offset.x,
                                                    offset.y,
                                                    isInNumber
                                                )
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            val isInNumber = isPointInNumber(numberPathBounds.value, change.position, IntSize(size.width.toInt(), size.height.toInt()))
                                            val newPoint = TracingPathPoints(
                                                change.position.x,
                                                change.position.y,
                                                isInNumber
                                            )
                                            currentPaths = currentPaths + newPoint
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            userPaths = userPaths + listOf(currentPaths)

                                            // Check stroke completion and overall number completion
                                            val (newStrokeStatus, newCompletionPercentage) = checkNumberCompletion(
                                                userPaths,
                                                numberPathBounds.value,
                                                currentNumber
                                            )

                                            strokeCompletionStatus = newStrokeStatus
                                            completionPercentage = newCompletionPercentage

                                            // Check if number is completed (all strokes traced)
                                            if (newCompletionPercentage >= 0.9f && !isNumberCompleted) {
                                                isNumberCompleted = true
                                                showSuccessMessage = true
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Draw only the points that are within the number
                            userPaths.forEach { points ->
                                // Split the path into segments based on isInNumber
                                val segments = mutableListOf<List<TracingPathPoints>>()
                                var currentSegment = mutableListOf<TracingPathPoints>()

                                for (i in points.indices) {
                                    val point = points[i]
                                    if (point.isInNumber) {
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

                                // Draw each segment that's inside the number
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

                            // Draw current path - only the points within the number
                            if (currentPaths.isNotEmpty()) {
                                // Similar logic for current path
                                val segments = mutableListOf<List<TracingPathPoints>>()
                                var currentSegment = mutableListOf<TracingPathPoints>()

                                for (i in currentPaths.indices) {
                                    val point = currentPaths[i]
                                    if (point.isInNumber) {
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
                        if (currentNumberIndex > 0) {
                            currentNumberIndex--
                            userPaths = listOf()
                            currentPaths = listOf()
                            completionPercentage = 0f
                            showSuccessMessage = false
                            isNumberCompleted = false
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
                        if (currentNumberIndex < numbers.size - 1) {
                            currentNumberIndex++
                            userPaths = listOf()
                            currentPaths = listOf()
                            completionPercentage = 0f
                            showSuccessMessage = false
                            isNumberCompleted = false
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
                        currentPaths = listOf()
                        completionPercentage = 0f
                        showSuccessMessage = false
                        isNumberCompleted = false
                        strokeCompletionStatus = listOf()
                    }
                ) {
                    Text("⟲", fontSize = 30.sp, color = Color.White)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNumberPath(
    number: String,
    outlineColor: Color,
    fillColor: Color,
    showDottedGuide: Boolean,
    strokeCompletionStatus: List<Boolean>
): List<Path> {
    val strokePaths = mutableListOf<Path>()

    when (number) {
        "0" -> {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radiusX = size.width * 0.3f
            val radiusY = size.height * 0.35f

            val ovalPath = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(
                    centerX - radiusX,
                    centerY - radiusY,
                    centerX + radiusX,
                    centerY + radiusY
                ))
            }
            strokePaths.add(ovalPath)
        }
        "1" -> {
            val centerX = size.width / 2
            val topY = size.height * 0.15f
            val bottomY = size.height * 0.85f

            val verticalLine = Path().apply {
                moveTo(centerX, topY)
                lineTo(centerX, bottomY)
            }
            strokePaths.add(verticalLine)
        }
        "2" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val middleY = size.height * 0.5f
            val bottomY = size.height * 0.8f

            val topCurve = Path().apply {
                moveTo(leftX, topY + size.height * 0.1f)
                cubicTo(leftX, topY, rightX, topY, rightX, middleY - size.height * 0.1f)
                lineTo(leftX, bottomY - size.height * 0.05f)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(topCurve)
        }
        "3" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val middleY = size.height * 0.5f
            val bottomY = size.height * 0.8f

            val topHalf = Path().apply {
                moveTo(leftX, topY)
                lineTo(rightX - size.width * 0.1f, topY)
                cubicTo(rightX, topY, rightX, middleY, leftX + size.width * 0.1f, middleY)
            }
            strokePaths.add(topHalf)

            val bottomHalf = Path().apply {
                moveTo(leftX + size.width * 0.1f, middleY)
                cubicTo(rightX, middleY, rightX, bottomY, leftX, bottomY)
            }
            strokePaths.add(bottomHalf)
        }
        "4" -> {
            val leftX = size.width * 0.25f
            val rightX = size.width * 0.75f
            val topY = size.height * 0.2f
            val middleY = size.height * 0.6f
            val bottomY = size.height * 0.85f

            val verticalLeft = Path().apply {
                moveTo(leftX, topY)
                lineTo(leftX, middleY)
                lineTo(rightX, middleY)
            }
            strokePaths.add(verticalLeft)

            val verticalRight = Path().apply {
                moveTo(rightX, topY)
                lineTo(rightX, bottomY)
            }
            strokePaths.add(verticalRight)
        }
        "5" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val middleY = size.height * 0.5f
            val bottomY = size.height * 0.8f

            val topAndSide = Path().apply {
                moveTo(rightX, topY)
                lineTo(leftX, topY)
                lineTo(leftX, middleY)
                lineTo(rightX - size.width * 0.1f, middleY)
                cubicTo(rightX, middleY, rightX, bottomY, leftX, bottomY)
            }
            strokePaths.add(topAndSide)
        }
        "6" -> {
            val leftX = size.width * 0.25f
            val rightX = size.width * 0.75f
            val topY = size.height * 0.2f
            val middleY = size.height * 0.5f
            val bottomY = size.height * 0.8f

            val mainPath = Path().apply {
                moveTo(rightX - size.width * 0.1f, topY)
                cubicTo(leftX, topY, leftX, middleY, leftX, middleY)
                cubicTo(leftX, bottomY, rightX, bottomY, rightX, middleY)
                cubicTo(rightX, middleY, leftX, middleY, leftX, middleY)
            }
            strokePaths.add(mainPath)
        }
        "7" -> {
            val leftX = size.width * 0.2f
            val rightX = size.width * 0.8f
            val topY = size.height * 0.2f
            val bottomY = size.height * 0.85f

            val sevenPath = Path().apply {
                moveTo(leftX, topY)
                lineTo(rightX, topY)
                lineTo(leftX + size.width * 0.2f, bottomY)
            }
            strokePaths.add(sevenPath)
        }
        "8" -> {
            val centerX = size.width / 2
            val topCenterY = size.height * 0.35f
            val bottomCenterY = size.height * 0.65f
            val radiusTop = size.width * 0.2f
            val radiusBottom = size.width * 0.25f

            val topCircle = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(
                    centerX - radiusTop,
                    size.height * 0.15f,
                    centerX + radiusTop,
                    topCenterY + radiusTop * 0.8f
                ))
            }
            strokePaths.add(topCircle)

            val bottomCircle = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(
                    centerX - radiusBottom,
                    bottomCenterY - radiusBottom * 0.8f,
                    centerX + radiusBottom,
                    size.height * 0.85f
                ))
            }
            strokePaths.add(bottomCircle)
        }
        "9" -> {
            val leftX = size.width * 0.25f
            val rightX = size.width * 0.75f
            val topY = size.height * 0.2f
            val middleY = size.height * 0.5f
            val bottomY = size.height * 0.8f

            val mainPath = Path().apply {
                moveTo(leftX + size.width * 0.1f, bottomY)
                cubicTo(rightX, bottomY, rightX, middleY, rightX, middleY)
                cubicTo(rightX, topY, leftX, topY, leftX, middleY)
                cubicTo(leftX, middleY, rightX, middleY, rightX, middleY)
            }
            strokePaths.add(mainPath)
        }
        else -> {
            // Default fallback
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
        }
    }

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

    return strokePaths
}

private fun isPointInNumber(numberPaths: List<Path>, point: Offset, size: IntSize): Boolean {
    val strokeWidth = 60f // Match the stroke width used for drawing

    // Check if the point is within any of the number's strokes
    for (composePath in numberPaths) {
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

private fun checkNumberCompletion(
    paths: List<List<TracingPathPoints>>,
    numberPaths: List<Path>,
    number: String
): Pair<List<Boolean>, Float> {
    if (numberPaths.isEmpty()) return Pair(listOf(), 0f)

    val strokeCompletionStatus = mutableListOf<Boolean>()
    var totalCompletionScore = 0f

    // Check each stroke of the number
    for (strokeIndex in numberPaths.indices) {
        val strokePath = numberPaths[strokeIndex]
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
                    if (userPoint.isInNumber) {
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
    val overallCompletion = if (numberPaths.isNotEmpty()) {
        totalCompletionScore / numberPaths.size
    } else {
        0f
    }

    return Pair(strokeCompletionStatus, overallCompletion)
}

@Preview
@Composable
fun NumberWritingScreenPreview() {
    NumberWritingScreen()
}