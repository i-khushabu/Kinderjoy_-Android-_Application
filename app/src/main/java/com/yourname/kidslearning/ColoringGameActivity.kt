package com.yourname.kidslearning

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.widget.Toast
import kotlin.math.min
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background

import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import java.io.File
import java.io.FileOutputStream


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

class ColoringGameActivity : ComponentActivity() {
    private var sessionStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)



        setContent {
            ColoringGameScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartTime = System.currentTimeMillis()
    }
    override fun onDestroy() {
        val sessionEndTime = System.currentTimeMillis()
        val durationMillis = sessionEndTime - sessionStartTime
        val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1) // Minimum 1 min
        saveProgressToFirebase("Coloring", durationMinutes) // or "ABC"
        AppSessionManager.cancelSession() // ✅ Important to stop timer when exiting
        super.onDestroy()
    }
}


@Composable
fun ColoringGameScreen() {
    val context = LocalContext.current
    val selectedColor = remember { mutableStateOf(Color.Red) }

    val images = listOf(
        R.drawable.outline_deer,
        R.drawable.outline_leaf,
        R.drawable.outline_plane,
        R.drawable.outline_turtle,
        R.drawable.outline_naruto,
        R.drawable.outline_dog,
        R.drawable.rabbitcolor,
        R.drawable.outline_queen,
        R.drawable.outline_unicorn,
        R.drawable.outline_mermaid,
        R.drawable.outline_pikachu,
        R.drawable.outline_leg
    )

    var selectedImage by remember { mutableStateOf(images[0]) }

    var bitmap by remember {
        mutableStateOf(createMutableBitmap(context, selectedImage))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val context = LocalContext.current

        // ✅ 1. Back Button placed first and layered on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(1f), // Ensure it's above other content
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = "Back",
                fontSize = 16.sp,
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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxHeight()
                    .padding(start = 64.dp, end = 1.dp) // Moved closer to canvas
            ) {
                images.forEach { imageId ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(2.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedImage = imageId
                                bitmap = createMutableBitmap(context, imageId)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = imageId),
                            contentDescription = "Select Image",
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxCanvasWidth = maxWidth
                val maxCanvasHeight = maxHeight

                val imageAspectRatio = bitmap.width.toFloat() / bitmap.height
                val canvasWidth: Dp
                val canvasHeight: Dp

                if (imageAspectRatio > maxCanvasWidth / maxCanvasHeight) {
                    canvasWidth = maxCanvasWidth
                    canvasHeight = maxCanvasWidth / imageAspectRatio
                } else {
                    canvasHeight = maxCanvasHeight
                    canvasWidth = maxCanvasHeight * imageAspectRatio
                }
                Box(
                    modifier = Modifier
                        .width(canvasWidth)
                        .height(canvasHeight)
                        .border(3.dp, Color.LightGray, RoundedCornerShape(12.dp)) // ✅ Added border
                        .background(Color.White, RoundedCornerShape(12.dp)) // Optional background
                )
                Canvas(
                    modifier = Modifier
                        .width(canvasWidth)
                        .height(canvasHeight)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val imageWidth = bitmap.width.toFloat()
                                val imageHeight = bitmap.height.toFloat()

                                val scaleFactor = min(
                                    canvasWidth.toPx() / imageWidth,
                                    canvasHeight.toPx() / imageHeight
                                )

                                val scaledWidth = imageWidth * scaleFactor
                                val scaledHeight = imageHeight * scaleFactor

                                val offsetX = (canvasWidth.toPx() - scaledWidth) / 2
                                val offsetY = (canvasHeight.toPx() - scaledHeight) / 2

                                val x = (((offset.x - offsetX) / scaleFactor).toInt()).coerceIn(0, bitmap.width - 1)
                                val y = (((offset.y - offsetY) / scaleFactor).toInt()).coerceIn(0, bitmap.height - 1)

                                CoroutineScope(Dispatchers.Default).launch {
                                    val updatedBitmap = floodFill(bitmap, x, y, selectedColor.value.toArgb())
                                    withContext(Dispatchers.Main) {
                                        bitmap = updatedBitmap
                                    }
                                }
                            }
                        }
                ) {
                    val imageBitmap = bitmap.asImageBitmap()
                    val imageWidth = imageBitmap.width.toFloat()
                    val imageHeight = imageBitmap.height.toFloat()

                    val scaleFactor = min(size.width / imageWidth, size.height / imageHeight)
                    val scaledWidth = imageWidth * scaleFactor
                    val scaledHeight = imageHeight * scaleFactor
                    val offsetX = (size.width - scaledWidth) / 2
                    val offsetY = (size.height - scaledHeight) / 2

                    drawIntoCanvas { canvas ->
                        val paint = Paint().asFrameworkPaint()
                        paint.isAntiAlias = true
                        paint.isDither = true

                        val srcRect = android.graphics.Rect(0, 0, imageBitmap.width, imageBitmap.height)
                        val dstRect = android.graphics.Rect(
                            offsetX.toInt(), offsetY.toInt(),
                            (offsetX + scaledWidth).toInt(), (offsetY + scaledHeight).toInt()
                        )

                        canvas.nativeCanvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 48.dp, end = 38.dp)
            ) {
                val colorList = listOf(
                    Color.Red, Color.Blue, Color.Green, Color.Yellow,
                    Color.Magenta, Color.Cyan, Color.Black, Color.Gray,
                    Color.LightGray, Color.White, Color(0xFFFFA500), Color(0xFF800080),
                    Color(0xFF00FF7F), Color(0xFFFF69B4), Color(0xFF8B4513), Color(0xFF4682B4) // ✅ New row
                )

                for (rowIndex in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (colIndex in 0 until 4) {
                            val color = colorList[rowIndex * 4 + colIndex]
                            val isSelected = selectedColor.value == color
                            val borderSize by animateDpAsState(if (isSelected) 4.dp else 0.dp)

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(borderSize, Color.White, RoundedCornerShape(6.dp))
                                    .background(color, RoundedCornerShape(6.dp))
                                    .clickable { selectedColor.value = color }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            bitmap = createMutableBitmap(context, selectedImage)
                            Toast.makeText(context, "Canvas Reset!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset")
                    }

                    Button(
                        onClick = {
                            saveImage(context, bitmap)
                            Toast.makeText(context, "Image Saved!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

fun createMutableBitmap(context: Context, drawableId: Int): Bitmap {
    val drawable = context.getDrawable(drawableId)
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    val width = 256
    val height = 256

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)

    return bitmap.copy(Bitmap.Config.ARGB_8888, true)
}

fun floodFill(bitmap: Bitmap, x: Int, y: Int, newColor: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)

    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val targetColor = pixels[y * width + x]
    if (targetColor == newColor) return bitmap

    val queue = ArrayDeque<Pair<Int, Int>>()
    queue.addLast(Pair(x, y))

    while (queue.isNotEmpty()) {
        val (px, py) = queue.removeFirst()

        if (px < 0 || py < 0 || px >= width || py >= height) continue
        if (pixels[py * width + px] != targetColor) continue

        pixels[py * width + px] = newColor

        queue.addLast(Pair(px + 1, py))
        queue.addLast(Pair(px - 1, py))
        queue.addLast(Pair(px, py + 1))
        queue.addLast(Pair(px, py - 1))
    }

    val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return newBitmap
}

fun isBorder(color: Int): Boolean {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    return (r < 80 && g < 80 && b < 80)
}

fun saveImage(context: android.content.Context, bitmap: Bitmap) {
    val file = File(context.getExternalFilesDir(null), "colored_image.png")
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    outputStream.flush()
    outputStream.close()
    Toast.makeText(context, "Image Saved!", Toast.LENGTH_SHORT).show()
}
