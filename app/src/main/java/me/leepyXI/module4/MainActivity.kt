package me.leepyXI.module4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.leepyXI.module4.ui.theme.MyApplicationTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompassScreen()
            }
        }
    }
}

@Composable
fun CompassScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    var azimuth by remember { mutableStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    
                    val degree = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuth = (degree + 360) % 360
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(Unit) {
        if (rotationSensor != null) {
            sensorManager.registerListener(
                sensorEventListener,
                rotationSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            hasSensor = false
        }

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Компас",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                modifier = Modifier.padding(top = 48.dp)
            )

            if (!hasSensor) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Устройство не поддерживает датчик ориентации",
                        color = Color.Red,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))

                CompassDisk(azimuth)

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Азимут: ${azimuth.roundToInt()}°",
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CompassDisk(azimuth: Float) {
    val animatedAzimuth by animateFloatAsState(
        targetValue = -azimuth,
        animationSpec = tween(durationMillis = 300),
        label = "azimuth_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .border(2.dp, Color.DarkGray, CircleShape)
            .background(Color(0xFF1E1E1E), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            
            rotate(animatedAzimuth, center) {
                drawCircle(Color.White, radius = 5f, center = Offset(center.x, center.y - size.height / 2 + 10f))

                val needleWidth = 40f
                val needleLength = size.height * 0.42f

                val northPath = Path().apply {
                    moveTo(center.x, center.y - needleLength)
                    lineTo(center.x - needleWidth / 2, center.y)
                    lineTo(center.x + needleWidth / 2, center.y)
                    close()
                }
                drawPath(northPath, Color.Red)

                val southPath = Path().apply {
                    moveTo(center.x, center.y + needleLength)
                    lineTo(center.x - needleWidth / 2, center.y)
                    lineTo(center.x + needleWidth / 2, center.y)
                    close()
                }
                drawPath(southPath, Color.White)
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                text = "N",
                color = Color.Red,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
