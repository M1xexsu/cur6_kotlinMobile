package me.leepyXI.module4

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class MyService : Service() {

    private var seconds = 0
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val channelId = "timerChannel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        showNotification(isInitial = true)
        startTimer()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun showNotification(isInitial: Boolean = false) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Таймер работает")
            .setContentText("Прошло секунд: $seconds")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        if (isInitial) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(notificationId, notification)
            }
        } else {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(notificationId, notification)
        }
    }

    private fun startTimer() {
        scope.launch {
            while (isActive) {
                delay(1000L)
                seconds++
                showNotification(isInitial = false)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}