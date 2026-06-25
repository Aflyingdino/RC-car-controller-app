package com.aflyingdino.rccarcontroller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.aflyingdino.rccarcontroller.ui.theme.AppSurface
import com.aflyingdino.rccarcontroller.ui.theme.BoostColor
import com.aflyingdino.rccarcontroller.ui.theme.BrakeColor
import com.aflyingdino.rccarcontroller.ui.theme.RCCarControllerTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RCCarControllerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppSurface
                ) {
                    ControllerDashboard()
                }
            }
        }
    }
}

@Composable
private fun ControllerDashboard() {
    var accelerating by remember { mutableStateOf(false) }
    var braking by remember { mutableStateOf(false) }
    val rawSteering = rememberGyroSteering()
    val steering by animateFloatAsState(
        targetValue = rawSteering,
        animationSpec = tween(160),
        label = "steering_smooth"
    )

    var speed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            speed = when {
                accelerating && !braking -> min(1f, speed + 0.018f)
                braking -> max(0f, speed - 0.035f)
                else -> max(0f, speed - 0.006f)
            }
            delay(16)
        }
    }

    val telemetrySteer = (steering * 100).toInt()
    val telemetrySpeed = (speed * 120).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08182A),
                        Color(0xFF0F2C46),
                        Color(0xFF071320)
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "RC DRIVE LAB",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFFE8F4FF)
            )

            Text(
                text = "Tilt your phone to steer. Hold boost or brake to control speed.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8D3E8)
            )

            DashboardCard(
                speed = speed,
                steering = steering,
                accelerating = accelerating,
                braking = braking,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            TelemetryStrip(
                speedKmh = telemetrySpeed,
                steerPercent = telemetrySteer,
                accelerating = accelerating,
                braking = braking
            )

            ControlButtons(
                onAcceleratePressed = { accelerating = true },
                onAccelerateReleased = { accelerating = false },
                onBrakePressed = {
                    braking = true
                    accelerating = false
                },
                onBrakeReleased = { braking = false }
            )
        }
    }
}

@Composable
private fun DashboardCard(
    speed: Float,
    steering: Float,
    accelerating: Boolean,
    braking: Boolean,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "dashboard_pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .background(
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF16375A).copy(alpha = 0.95f),
                            Color(0xFF0A1A2B).copy(alpha = 0.98f)
                        ),
                        radius = 1200f
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            F1Visualization(
                speed = speed,
                steering = steering,
                accelerating = accelerating,
                braking = braking,
                pulseAlpha = pulseAlpha,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun F1Visualization(
    speed: Float,
    steering: Float,
    accelerating: Boolean,
    braking: Boolean,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val roadFlow = rememberInfiniteTransition(label = "road_flow")
    val lanePhase by roadFlow.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
        label = "lane_phase"
    )

    Canvas(modifier = modifier) {
        val cardW = size.width
        val cardH = size.height
        val center = Offset(cardW / 2f + steering * 90f, cardH * 0.54f)
        val carLength = cardH * 0.5f
        val carWidth = cardW * 0.2f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0E2740),
                    Color(0xFF081524)
                )
            ),
            cornerRadius = CornerRadius(40f, 40f)
        )

        val lineCount = 12
        for (i in 0 until lineCount) {
            val baseY = (cardH / lineCount) * i
            val y = (baseY + lanePhase * (100f + speed * 180f) * 4f) % cardH
            val widthBoost = 1f + ((y / cardH) * 2.2f)
            drawRoundRect(
                color = Color(0xFF9EC7E9).copy(alpha = 0.14f + 0.12f * speed),
                topLeft = Offset(cardW * 0.485f - widthBoost * 3f, y),
                size = Size(widthBoost * 6f, 32f + widthBoost * 3f),
                cornerRadius = CornerRadius(20f, 20f)
            )
        }

        drawCircle(
            color = Color.Black.copy(alpha = 0.28f),
            radius = carWidth * 0.9f,
            center = center + Offset(0f, carLength * 0.18f)
        )

        withTransform({
            rotate(degrees = steering * 10f, pivot = center)
        }) {
            if (accelerating) {
                for (i in 1..4) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BoostColor.copy(alpha = 0.45f - (i * 0.07f)),
                                Color.Transparent
                            ),
                            radius = 120f + i * 18f
                        ),
                        topLeft = Offset(
                            center.x - carWidth * 0.34f,
                            center.y + carLength * 0.29f + (i * 8f)
                        ),
                        size = Size(carWidth * 0.68f, carLength * 0.26f)
                    )
                }
            }

            val bodyPath = Path().apply {
                moveTo(center.x, center.y - carLength * 0.45f)
                cubicTo(
                    center.x + carWidth * 0.42f,
                    center.y - carLength * 0.34f,
                    center.x + carWidth * 0.50f,
                    center.y + carLength * 0.22f,
                    center.x + carWidth * 0.20f,
                    center.y + carLength * 0.45f
                )
                lineTo(center.x - carWidth * 0.20f, center.y + carLength * 0.45f)
                cubicTo(
                    center.x - carWidth * 0.50f,
                    center.y + carLength * 0.22f,
                    center.x - carWidth * 0.42f,
                    center.y - carLength * 0.34f,
                    center.x,
                    center.y - carLength * 0.45f
                )
                close()
            }

            drawPath(
                path = bodyPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF2FAFF),
                        Color(0xFF96BFD9),
                        Color(0xFF1B405C)
                    ),
                    start = Offset(center.x - carWidth, center.y - carLength),
                    end = Offset(center.x + carWidth, center.y + carLength)
                )
            )

            drawPath(
                path = bodyPath,
                color = Color(0xFFCCE6F8).copy(alpha = 0.45f),
                style = Stroke(width = 4f)
            )

            drawRoundRect(
                color = Color(0xFF04111C),
                topLeft = Offset(center.x - carWidth * 0.58f, center.y - carLength * 0.42f),
                size = Size(carWidth * 1.16f, carLength * 0.06f),
                cornerRadius = CornerRadius(16f, 16f)
            )

            drawRoundRect(
                color = Color(0xFF031321),
                topLeft = Offset(center.x - carWidth * 0.72f, center.y + carLength * 0.31f),
                size = Size(carWidth * 1.44f, carLength * 0.08f),
                cornerRadius = CornerRadius(16f, 16f)
            )

            val wheelColor = Color(0xFF131A20)
            val wheelPositions = listOf(
                Offset(center.x - carWidth * 0.55f, center.y - carLength * 0.23f),
                Offset(center.x + carWidth * 0.25f, center.y - carLength * 0.23f),
                Offset(center.x - carWidth * 0.55f, center.y + carLength * 0.16f),
                Offset(center.x + carWidth * 0.25f, center.y + carLength * 0.16f)
            )
            wheelPositions.forEach { wheel ->
                drawRoundRect(
                    color = wheelColor,
                    topLeft = wheel,
                    size = Size(carWidth * 0.30f, carLength * 0.17f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = Color(0xFF3A444D),
                    topLeft = wheel + Offset(carWidth * 0.04f, carLength * 0.03f),
                    size = Size(carWidth * 0.22f, carLength * 0.11f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f + speed * 0.3f),
                        Color.Transparent
                    ),
                    radius = 90f
                ),
                topLeft = Offset(center.x - carWidth * 0.32f, center.y - carLength * 0.28f),
                size = Size(carWidth * 0.64f, carLength * 0.18f)
            )

            if (braking) {
                drawRoundRect(
                    color = BrakeColor.copy(alpha = 0.45f + pulseAlpha),
                    topLeft = Offset(center.x - carWidth * 0.17f, center.y + carLength * 0.44f),
                    size = Size(carWidth * 0.34f, carLength * 0.04f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        val ringColor = when {
            braking -> BrakeColor
            accelerating -> BoostColor
            else -> Color(0xFF8CA9C4)
        }

        drawArc(
            color = ringColor.copy(alpha = 0.75f),
            startAngle = 120f,
            sweepAngle = 300f * speed,
            useCenter = false,
            topLeft = Offset(cardW * 0.06f, cardH * 0.08f),
            size = Size(cardW * 0.24f, cardW * 0.24f),
            style = Stroke(width = 12f)
        )

        drawArc(
            color = Color(0x334C6F91),
            startAngle = 120f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(cardW * 0.06f, cardH * 0.08f),
            size = Size(cardW * 0.24f, cardW * 0.24f),
            style = Stroke(width = 4f)
        )
    }
}

@Composable
private fun TelemetryStrip(
    speedKmh: Int,
    steerPercent: Int,
    accelerating: Boolean,
    braking: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x1FFFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryCell(label = "Speed", value = "$speedKmh km/h", accent = Color(0xFF8DEBFF))
        TelemetryCell(label = "Steer", value = "$steerPercent%", accent = Color(0xFF9FEA9A))
        val mode = when {
            braking -> "BRAKE"
            accelerating -> "BOOST"
            else -> "COAST"
        }
        val modeColor = when {
            braking -> BrakeColor
            accelerating -> BoostColor
            else -> Color(0xFFC7D8E8)
        }
        TelemetryCell(label = "Mode", value = mode, accent = modeColor)
    }
}

@Composable
private fun TelemetryCell(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            color = Color(0xFFA9C3D9),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = accent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ControlButtons(
    onAcceleratePressed: () -> Unit,
    onAccelerateReleased: () -> Unit,
    onBrakePressed: () -> Unit,
    onBrakeReleased: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PressHoldButton(
            title = "ACCELERATE",
            subtitle = "Boost",
            activeColor = BoostColor,
            modifier = Modifier.weight(1f),
            onPress = onAcceleratePressed,
            onRelease = onAccelerateReleased
        )

        PressHoldButton(
            title = "BRAKE",
            subtitle = "Stop",
            activeColor = BrakeColor,
            modifier = Modifier.weight(1f),
            onPress = onBrakePressed,
            onRelease = onBrakeReleased
        )
    }
}

@Composable
private fun PressHoldButton(
    title: String,
    subtitle: String,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(94.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        activeColor.copy(alpha = if (pressed) 0.85f else 0.45f),
                        Color(0xFF12273A)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val isDown = event.changes.any { it.pressed }
                        if (isDown && !pressed) {
                            pressed = true
                            onPress()
                        }
                        if (!isDown && pressed) {
                            pressed = false
                            onRelease()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFFD5E5F2),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun rememberGyroSteering(): Float {
    val context = androidx.compose.ui.platform.LocalContext.current
    var steering by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (gyro == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val yawRate = event.values[2]
                    val normalized = (yawRate / 2.8f).coerceIn(-1.2f, 1.2f)
                    steering = (steering * 0.86f + normalized * 0.14f).coerceIn(-1f, 1f)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            steering *= 0.985f
            if (abs(steering) < 0.01f) steering = 0f
            delay(16)
        }
    }

    return steering
}
