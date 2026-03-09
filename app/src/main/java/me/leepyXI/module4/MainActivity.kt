package me.leepyXI.module4

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import me.leepyXI.module4.ui.theme.MyApplicationTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PillReminderScreen()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "PILL_REMINDER_CHANNEL",
                "Напоминания о таблетках",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления для приема лекарств"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun PillReminderScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("pill_prefs", Context.MODE_PRIVATE) }
    
    var isEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("is_enabled", false)) }
    var nextReminderText by remember { mutableStateOf(sharedPrefs.getString("next_reminder", "") ?: "") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Напоминание о таблетке",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Color.Green else Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEnabled) "Включено" else "Выключено",
                fontSize = 18.sp,
                color = if (isEnabled) Color.Black else Color.Gray
            )
        }

        if (isEnabled && nextReminderText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = nextReminderText,
                fontSize = 16.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@Button
                }

                if (isEnabled) {
                    cancelPillAlarm(context)
                    isEnabled = false
                    nextReminderText = ""
                    sharedPrefs.edit().putBoolean("is_enabled", false).remove("next_reminder").apply()
                } else {
                    val scheduledDay = schedulePillAlarm(context)
                    isEnabled = true
                    nextReminderText = "Следующее напоминание: $scheduledDay в 20:00"
                    sharedPrefs.edit()
                        .putBoolean("is_enabled", true)
                        .putString("next_reminder", nextReminderText)
                        .apply()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) Color.LightGray else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isEnabled) "Выключить напоминание" else "Включить напоминание",
                fontSize = 18.sp,
                color = if (isEnabled) Color.Black else Color.White
            )
        }
    }
}

fun schedulePillAlarm(context: Context): String {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    val isToday = calendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return if (isToday) "сегодня" else "завтра"
}

fun cancelPillAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, "PILL_REMINDER_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Время принять таблетку!")
            .setContentText("Пора принять ваше лекарство (20:00)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        val nextTime = schedulePillAlarm(context)
        val sharedPrefs = context.getSharedPreferences("pill_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("next_reminder", "Следующее напоминание: $nextTime в 20:00")
            .apply()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPrefs = context.getSharedPreferences("pill_prefs", Context.MODE_PRIVATE)
            if (sharedPrefs.getBoolean("is_enabled", false)) {
                val nextTime = schedulePillAlarm(context)
                sharedPrefs.edit()
                    .putString("next_reminder", "Следующее напоминание: $nextTime в 20:00")
                    .apply()
            }
        }
    }
}
