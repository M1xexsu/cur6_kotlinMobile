package me.leepyXI.module4

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.delay
import me.leepyXI.module4.ui.theme.MyApplicationTheme
import java.util.concurrent.CopyOnWriteArrayList

data class CityWeather(
    val name: String,
    val temp: Int? = null,
    val condition: String? = null,
    val isDone: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather Channel"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("WEATHER_CHANNEL", name, importance)
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val workManager = WorkManager.getInstance(context)

        val weatherWorkInfos by workManager.getWorkInfosByTagLiveData("weather_download").observeAsState(emptyList())
        val reportWorkInfos by workManager.getWorkInfosByTagLiveData("report_tag").observeAsState(emptyList())

        val cities = listOf("Москва", "Лондон", "Нью-Йорк")

        val weatherList = remember(weatherWorkInfos) {
            cities.map { cityName ->
                val info = weatherWorkInfos.find { it.tags.contains("city_$cityName") }
                if (info != null && info.state == WorkInfo.State.SUCCEEDED) {
                    CityWeather(
                        name = cityName,
                        temp = info.outputData.getInt("TEMP", 0),
                        condition = info.outputData.getString("CONDITION"),
                        isDone = true
                    )
                } else {
                    CityWeather(name = cityName, isDone = false)
                }
            }
        }

        val reportInfo = reportWorkInfos.firstOrNull()
        val isAllDone = reportInfo?.state == WorkInfo.State.SUCCEEDED
        val avgTemp = reportInfo?.outputData?.getInt("AVG_TEMP", 0)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF0F8FF)
        ) {
            Column(
                modifier = Modifier
                    .systemBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Прогноз погоды",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                
                Text(
                    text = if (isAllDone) "Все данные получены!" else "Загрузка данных...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(weatherList) { weather ->
                        WeatherCard(weather)
                    }

                    if (isAllDone) {
                        item {
                            ReportCard(weatherList, avgTemp ?: 0)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { startWeatherWork(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006080)),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Собрать прогноз", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }

    @Composable
    fun WeatherCard(weather: CityWeather) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE6ED)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = weather.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = if (weather.isDone) "Готово" else "В процессе...",
                        color = if (weather.isDone) Color(0xFF4682B4) else Color.Gray,
                        fontSize = 14.sp
                    )
                }
                if (weather.isDone) {
                    Text(
                        text = "${weather.temp}°C",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            }
        }
    }

    @Composable
    fun ReportCard(weatherList: List<CityWeather>, avgTemp: Int) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE6ED)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Итоговый прогноз:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                weatherList.forEach { weather ->
                    Text(
                        text = "${weather.name}: ${weather.temp}°C, ${weather.condition ?: ""}",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Средняя температура: $avgTemp°C",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }

    private fun startWeatherWork(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.pruneWork()

        val cities = listOf("Москва", "Лондон", "Нью-Йорк")
        val weatherRequests = cities.map { city ->
            OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(workDataOf("CITY" to city))
                .addTag("weather_download")
                .addTag("city_$city")
                .build()
        }

        val reportRequest = OneTimeWorkRequestBuilder<ReportWorker>()
            .setInputMerger(ArrayCreatingInputMerger::class.java)
            .addTag("report_tag")
            .build()

        workManager.beginUniqueWork("weather_job", ExistingWorkPolicy.REPLACE, weatherRequests)
            .then(reportRequest)
            .enqueue()
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val city = inputData.getString("CITY") ?: "Unknown"

        setForeground(createForegroundInfo("Загружаем погоду для $city…"))

        delay(2000L + (0..3000).random())
        
        val temp = (-10..25).random()
        val conditions = listOf("ясно", "дождь", "облачно", "снег")
        val condition = conditions.random()

        return Result.success(workDataOf(
            "TEMP" to temp,
            "CONDITION" to condition
        ))
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "WEATHER_CHANNEL")
            .setContentTitle("Прогноз погоды")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_menu_day)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(101, notification)
        }
    }
}

class ReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo("Формируем отчёт…"))
        delay(1500L)

        val temps = inputData.getIntArray("TEMP")
        val avgTemp = if (temps != null && temps.isNotEmpty()) {
            temps.average().toInt()
        } else {
            0
        }

        return Result.success(workDataOf("AVG_TEMP" to avgTemp))
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "WEATHER_CHANNEL")
            .setContentTitle("Прогноз погоды")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_day)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(101, notification)
        }
    }
}
