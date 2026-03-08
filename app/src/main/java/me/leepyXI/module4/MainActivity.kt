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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.leepyXI.module4.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var myService: MyService? = null
    private var isBound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MyService.LocalBinder
            myService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            myService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val randomNumber by if (isBound) {
                    myService?.randomNumber?.collectAsState() ?: remember { mutableStateOf(0) }
                } else {
                    remember { mutableStateOf(0) }
                }

                MainScreen(
                    randomNumber = randomNumber,
                    isBound = isBound,
                    onConnect = { bindMyService() },
                    onDisconnect = { unbindMyService() }
                )
            }
        }
    }

    private fun bindMyService() {
        Intent(this, MyService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindMyService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
            myService = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindMyService()
    }
}

@Composable
fun MainScreen(
    randomNumber: Int,
    isBound: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isBound) "$randomNumber" else "---",
            style = MaterialTheme.typography.displayLarge
        )

        Text(text = if (isBound) "Сервис подключен" else "Сервис отключен")

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Button(onClick = onConnect, enabled = !isBound) {
                Text("Подключиться")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onDisconnect, enabled = isBound) {
                Text("Отключиться")
            }
        }
    }
}