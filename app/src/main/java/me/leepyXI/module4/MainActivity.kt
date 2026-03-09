package me.leepyXI.module4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.leepyXI.module4.ui.theme.MyApplicationTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CurrencyScreen()
            }
        }
    }
}

class CurrencyViewModel : ViewModel() {
    private val _rate = MutableStateFlow(90.5)
    val rate = _rate.asStateFlow()

    private val _previousRate = MutableStateFlow(90.5)
    val previousRate = _previousRate.asStateFlow()

    init {
        startAutoUpdate()
    }

    private fun startAutoUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                updateRate()
            }
        }
    }

    fun updateRate() {
        val oldRate = _rate.value
        _previousRate.value = oldRate
        val newRate = 88.5 + Random.nextDouble() * 4.0
        _rate.value = String.format("%.2f", newRate).replace(",", ".").toDouble()
    }
}

@Composable
fun CurrencyScreen(viewModel: CurrencyViewModel = viewModel()) {
    val currentRate by viewModel.rate.collectAsState()
    val previousRate by viewModel.previousRate.collectAsState()

    val isHigher = currentRate >= previousRate
    val arrowIcon = if (isHigher) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val arrowColor = if (isHigher) Color.Green else Color.Red

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Курс USD/RUB",
                fontSize = 20.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currentRate ₽",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = arrowIcon,
                    contentDescription = null,
                    tint = arrowColor,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { viewModel.updateRate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Обновить сейчас", fontSize = 18.sp)
            }
        }
    }
}
