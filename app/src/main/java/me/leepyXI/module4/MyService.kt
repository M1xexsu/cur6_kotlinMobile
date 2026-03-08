package me.leepyXI.module4

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class MyService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _randomNumber = MutableStateFlow(0)
    val randomNumber = _randomNumber.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): MyService = this@MyService
    }

    override fun onCreate() {
        super.onCreate()
        startGeneratingNumbers()
    }

    private fun startGeneratingNumbers() {
        scope.launch {
            while (isActive) {
                _randomNumber.value = Random.nextInt(0, 101)
                delay(1000L)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
