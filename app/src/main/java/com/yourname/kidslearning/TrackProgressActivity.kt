package com.yourname.kidslearning

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.compose.material.icons.automirrored.filled.ArrowBack

class TrackProgressActivity : ComponentActivity() {
    private var forceRefresh: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode(window)
        setContent {
            TrackProgressScreen { refresher ->
                forceRefresh = refresher
            }
        }
    }

    override fun onResume() {
        super.onResume()
        forceRefresh?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackProgressScreen(onRegisterRefresher: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    var sectionData by remember { mutableStateOf<Map<String, Int>?>(null) }
    var chartType by remember { mutableStateOf("\ud83d\udcc8 Line Chart") }
    val chartTypes = listOf("\ud83d\udcc8 Line Chart", "\ud83d\udcca Bar Chart", "\ud83e\udd67 Pie Chart")

    // Load from Firebase
    fun loadProgress() {
        fetchSectionProgressFromFirebase { result ->
            if (result.isEmpty()) {
                Toast.makeText(context, "No progress data found.", Toast.LENGTH_SHORT).show()
            }
            sectionData = result
        }
    }

    // Load once on screen start and register refresher
    LaunchedEffect(Unit) {
        loadProgress()
        onRegisterRefresher { loadProgress() }
    }

    val activity = LocalActivity.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { activity?.finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                        Text("Track Progress", modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                resetProgressDataToDefault()
                                Toast.makeText(context, "Progress reset.", Toast.LENGTH_SHORT).show()
                                loadProgress()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CBAC8)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("\ud83d\udd04 Reset", color = Color.White)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = chartType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Chart Type") },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    chartTypes.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                chartType = it
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    sectionData == null -> CircularProgressIndicator()
                    sectionData!!.isEmpty() -> Text("No progress data available.")
                    else -> {
                        val data = sectionData!!
                        val dataPoints = data.entries.mapIndexed { index, entry ->
                            index.toFloat() to entry.value.toFloat()
                        }
                        val labels = data.keys.toList()
                        val values = data.values.map { it.toFloat() }

                        AnimatedContent(targetState = chartType, label = "ChartSwitch") { type ->
                            when (type) {
                                "\ud83d\udcc8 Line Chart" -> LineChartView(context, dataPoints, labels)
                                "\ud83d\udcca Bar Chart" -> BarChartView(context, dataPoints, labels)
                                "\ud83e\udd67 Pie Chart" -> PieChartView(context, values, labels)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineChartView(context: Context, dataPoints: List<Pair<Float, Float>>, labels: List<String>) {
    AndroidView(
        factory = {
            LineChart(context).apply {
                description.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                axisRight.isEnabled = false
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            }
        },
        update = { chart ->
            val entries = dataPoints.map { Entry(it.first, it.second) }
            val dataSet = LineDataSet(entries, "Progress").apply {
                mode = LineDataSet.Mode.LINEAR
                setColors(*ColorTemplate.MATERIAL_COLORS)
                lineWidth = 3f
                circleRadius = 5f
                setDrawValues(true)
                setDrawFilled(true)
                fillColor = ColorTemplate.getHoloBlue()
                fillAlpha = 100
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Composable
fun BarChartView(context: Context, dataPoints: List<Pair<Float, Float>>, labels: List<String>) {
    AndroidView(
        factory = {
            BarChart(context).apply {
                description.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                axisRight.isEnabled = false
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            }
        },
        update = { chart ->
            val entries = dataPoints.map { BarEntry(it.first, it.second) }
            val dataSet = BarDataSet(entries, "Progress").apply {
                setColors(*ColorTemplate.COLORFUL_COLORS)
                valueTextSize = 10f
            }
            chart.data = BarData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Composable
fun PieChartView(context: Context, values: List<Float>, labels: List<String>) {
    AndroidView(
        factory = {
            PieChart(context).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(android.graphics.Color.WHITE)
            }
        },
        update = { chart ->
            val entries = values.mapIndexed { index, value ->
                PieEntry(value, labels.getOrNull(index) ?: "Unknown")
            }
            val dataSet = PieDataSet(entries, "Progress").apply {
                setColors(*ColorTemplate.JOYFUL_COLORS)
                valueTextSize = 14f
            }
            chart.data = PieData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
