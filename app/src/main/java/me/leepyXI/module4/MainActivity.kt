package me.leepyXI.module4

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class entity(
    val id: Int,
    val full_name: String,
    val description: String,
    val stargazers_count: Int,
    val language: String
)


@Composable
fun SearchScreen(modifier: Modifier) {
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val allItems = remember { mutableStateListOf<entity>() }
    val results = remember { mutableStateListOf<entity>() }

    var showResults by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        showResults = false
        try {
            val raw = withContext(Dispatchers.IO) {
                context.assets.open("repos.json").bufferedReader().use { it.readText() }
            }
            val arr = JSONArray(raw)
            val parsed = mutableListOf<entity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optInt("id", i)
                val fullName = o.optString("full_name", o.optString("name", ""))
                val description = o.optString("description", "")
                val stars = o.optInt("stargazers_count", 0)
                val language = o.optString("language", "")
                parsed.add(entity(id, fullName, description, stars, language))
            }
            allItems.clear()
            allItems.addAll(parsed)
            results.clear()
            results.addAll(parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            allItems.clear()
            results.clear()
        }

        isLoading = false
        showResults = true
    }

    val debounced = remember {
        debounce<String>(500L) { query ->
            isLoading = true
            showResults = false

            val filtered = withContext(Dispatchers.Default) {
                if (query.isBlank()) {
                    allItems.toList()
                } else {
                    allItems.filter { it.full_name.contains(query, ignoreCase = true) }
                }
            }

            results.clear()
            results.addAll(filtered)

            isLoading = false
            showResults = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextField(
            value = text,
            onValueChange = {
                text = it
                debounced(scope, it)
            },
            label = { Text("Введите название") },
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = isLoading to showResults,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            },
            label = "state_transition"
        ) { (loading, resultsVisible) ->
            when {
                loading -> {
                    LoadingScreen()
                }
                resultsVisible -> {
                    ResultsList(text, results)
                }
            }
        }
    }
}


@Composable
fun ResultsList(text: String, items: List<entity>) {
    val filtered = if (text.isBlank()) items else items.filter { it.full_name.contains(text, ignoreCase = true) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        items(filtered) { item ->
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = item.full_name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (item.description.isNotBlank()) item.description else "No description",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "⭐ ${item.stargazers_count} • ${if (item.language.isNotBlank()) item.language else "—"}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка данных...")
        }
    }
}



@Suppress("UNUSED_VARIABLE", "UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED", "REDUNDANT_ASSIGNMENT")
fun <T> debounce(
    waitMs: Long = 500L,
    destinationFunction: suspend (T) -> Unit
): (CoroutineScope, T) -> Unit {
    var debounceJob: Job? = null
    return { coroutineScope: CoroutineScope, param: T ->
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(waitMs)
            destinationFunction(param)
        }
    }
}
