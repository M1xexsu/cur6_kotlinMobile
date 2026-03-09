package me.leepyXI.module4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import me.leepyXI.module4.ui.theme.MyApplicationTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AnimalFactsScreen()
            }
        }
    }
}

class AnimalFactsViewModel : ViewModel() {
    private val facts = listOf(
        "Олени могут плавать со скоростью до 6 км/ч.",
        "Сердце кита бьется всего 9 раз в минуту.",
        "Слоны — единственные животные, которые не умеют прыгать.",
        "Отпечатки пальцев коал практически не отличимы от человеческих.",
        "У улитки около 25 000 зубов.",
        "Медведи гризли могут учуять запах на расстоянии до 30 километров.",
        "Морские коньки — единственные рыбы, которые плавают вертикально.",
        "У осьминога три сердца.",
        "Жирафы не имеют голосовых связок.",
        "Крокодилы глотают камни, чтобы глубже нырять.",
        "Пингвины могут подпрыгивать в воздухе на высоту до 2 метров.",
        "У кошек 32 мышцы в каждом ухе.",
        "Тигры имеют полосатую кожу, а не только мех.",
        "Бегемоты могут открывать пасть на 180 градусов.",
        "Пчелы могут узнавать человеческие лица."
    )

    private val _uiState = MutableStateFlow<FactUiState>(FactUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun loadNewFact() {
        viewModelScope.launch {
            _uiState.value = FactUiState.Loading
            getRandomFact().collect { fact ->
                _uiState.value = FactUiState.Success(fact)
            }
        }
    }

    private fun getRandomFact(): Flow<String> = flow {
        delay(Random.nextLong(1500, 3001))
        emit(facts.random())
    }
}

sealed class FactUiState {
    object Idle : FactUiState()
    object Loading : FactUiState()
    data class Success(val fact: String) : FactUiState()
}

@Composable
fun AnimalFactsScreen(viewModel: AnimalFactsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()


        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Интересные факты о животных",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it }
                    },
                    label = "fact_animation"
                ) { state ->
                    when (state) {
                        is FactUiState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is FactUiState.Success -> {
                            FactCard(state.fact)
                        }
                        is FactUiState.Idle -> {
                            Text(
                                "Нажмите кнопку, чтобы узнать что-то новое!",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.loadNewFact() },
                enabled = uiState !is FactUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Новый факт!", fontSize = 18.sp)
            }
        }
    }


@Composable
fun FactCard(fact: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = fact,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
