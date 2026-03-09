package me.leepyXI.module4
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import me.leepyXI.module4.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen(){
        var status by remember {mutableStateOf("Нажмите \"Начать\" для...")}
        val context = LocalContext.current
        var _enb by remember {mutableStateOf(true)}
        var prg by remember {mutableStateOf(0.0f)}


        Box(Modifier
            .systemBarsPadding()
            .fillMaxSize())
        {
            Column(Modifier.align(Alignment.Center)) {
                Text(text = status,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally),
                    progress = prg)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = _enb,
                    onClick = {
                        _enb = false
                        status = "Сжимаем фото…"
                        val wm = WorkManager.getInstance(context)

                        val s1 = OneTimeWorkRequestBuilder<Stage1Worker>().build()
                        val s2 = OneTimeWorkRequestBuilder<Stage2Worker>().build()
                        val s3 = OneTimeWorkRequestBuilder<Stage3Worker>().build()

                        wm.beginWith(s1).then(s2).then(s3).enqueue()

                        listOf(s1, s2, s3).forEach { request ->
                            wm.getWorkInfoByIdLiveData(request.id).observe(this@MainActivity) { info ->
                                if (info?.state == WorkInfo.State.SUCCEEDED) {
                                    status = info.outputData.getString("result") ?: status
                                    prg = info.outputData.getFloat("progress", 0.0f)
                                }
                            }
                        }
                        _enb = true
                    }
                ) {
                    Text(text = "Начать")
                }
            }
        }
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun GreetingPreview() {
        MyApplicationTheme {
            MainScreen()
        }
    }
}

class Stage1Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params)
{
    override suspend fun doWork(): Result {
        val res = Data.Builder()
        try {
            delay(1000L)
            res.putString("result", "Добавляем водяной знак…").putFloat("progress", 0.3f)

        }
        catch (e: Exception) {
            return Result.failure()
        }
        return Result.success(res.build())
    }
}

class Stage2Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params)
{
    override suspend fun doWork(): Result {
        val res = Data.Builder()
        try {
            delay(1000L)
            res.putString("result", "Загружаем...").putFloat("progress", 0.6f)

        }
        catch (e: Exception) {
            return Result.failure()
        }
        return Result.success(res.build())
    }
}

class Stage3Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params)
{
    override suspend fun doWork(): Result {
        val res = Data.Builder()
        try {
            delay(1000L)
            res.putString("result", "Готово! Фото загружено").putFloat("progress", 1.0f)
        }
        catch (e: Exception) {
            return Result.failure()
        }
        return Result.success(res.build())
    }
}