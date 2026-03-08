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

//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//
//        showNotification(isInitial = true)
//        startTimer()
//    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        seconds = intent?.getIntExtra("time", 0) ?: 0
        createNotificationChannel()
        startTimer(seconds)
        return START_NOT_STICKY
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Время вышло")
            .setContentText("Прошло секунд: $seconds")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()


        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)

        stopService(Intent(this, MyService::class.java))
    }

    private fun startTimer(time: Int = 0) {
        scope.launch {
            delay(time * 1000L)
            showNotification()
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}