@file:OptIn(ExperimentalMaterial3Api::class)

package me.leepyXI.module4

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.leepyXI.module4.ui.theme.MyApplicationTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import org.json.JSONArray
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlin.collections.addAll
import kotlin.collections.forEachIndexed
import kotlin.collections.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(Modifier.systemBarsPadding())
            }
        }
    }
}

sealed class LoadingStatus {
    object Loading : LoadingStatus()
    object Ready : LoadingStatus()
    data class Error(val message: String) : LoadingStatus()
}


data class PostUI(
    val id: Int,
    val name: String,
    val body: String,
    val avatarUrl: String = "",
    val comments: List<Comments> = emptyList(),
    val status: LoadingStatus = LoadingStatus.Loading
)

data class Comments(
    val postId: Int,
    val id: Int,
    val name: String,
    val body: String
)

suspend fun parsePosts(jsonString: String): List<PostUI> = withContext(Dispatchers.IO) {
    val list = mutableListOf<PostUI>()
    val jsonArray = JSONArray(jsonString)
    for (i in 0 until jsonArray.length()) {
        try {
            val obj = jsonArray.getJSONObject(i)
            val id = if (obj.has("postId")) obj.optInt("postId", -1) else obj.optInt("id", -1)
            if (id == -1) continue
            val name = when {
                obj.has("title") -> obj.optString("title", "")
                obj.has("name") -> obj.optString("name", "")
                else -> ""
            }
            val body = obj.optString("body", "")
            val avatar = obj.optString("avatarUrl", "")

            list.add(
                PostUI(
                    id = id,
                    name = name.ifEmpty { "Пост #$id" },
                    body = body,
                    avatarUrl = avatar
                )
            )
        } catch (_: Exception) {
        }
    }
    list
}

suspend fun fetchCommentsForPost(postId: Int, context: Context): List<Comments> = withContext(Dispatchers.IO) {
    delay(1500L + (500..3000).random())

    try {
        val raw = context.assets.open("comments.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(raw)
        val result = mutableListOf<Comments>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val pId = obj.optInt("postId", -1)
            if (pId != postId) continue
            val id = obj.optInt("id", -1)
            val name = obj.optString("name", "")
            val body = obj.optString("body", "")
            if (id != -1) {
                result.add(Comments(postId = pId, id = id, name = name, body = body))
            }
        }
        result
    } catch (e: Exception) {
        emptyList()
    }
}



@Composable
fun MainScreen(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val posts = remember { mutableStateListOf<PostUI>() }
    var isRefreshing by remember { mutableStateOf(false) }

    var currentLoadJob by remember { mutableStateOf<Job?>(null) }

    fun loadData() {
        currentLoadJob?.cancel()
        posts.clear()
        isRefreshing = true

        currentLoadJob = scope.launch {
            try {
                val rawJson = withContext(Dispatchers.IO) {
                    context.assets.open("social_posts.json").bufferedReader().use { it.readText() }
                }
                val basePosts = parsePosts(rawJson)

                posts.addAll(basePosts)
                isRefreshing = false
                posts.forEachIndexed { index, post ->
                    launch {
                        try {
                            supervisorScope {
                                val avatarDeferred = async {
                                    delay(1000L + (500..2000).random())
                                    post.avatarUrl
                                }
                                val commentsDeferred = async {
                                    fetchCommentsForPost(post.id, context)
                                }

                                val avatar = avatarDeferred.await()
                                val comments = commentsDeferred.await()

                                posts[index] = posts[index].copy(
                                    avatarUrl = avatar,
                                    comments = comments,
                                    status = LoadingStatus.Ready
                                )
                            }
                        } catch (e: Exception) {
                            posts[index] = posts[index].copy(
                                status = LoadingStatus.Error(e.message ?: "Ошибка")
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    RefreshList(
        posts = posts,
        isLoading = isRefreshing,
        onRefresh = { loadData() },
        modifier = modifier
    )
}




@Composable
fun RefreshList(posts: List<PostUI>, isLoading: Boolean, onRefresh: () -> Unit, modifier: Modifier) {
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (posts.isEmpty() && !isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Нет постов", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Потяните вниз, чтобы обновить", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(posts, key = { it.id }) { post ->
                    PostItem(post = post)
                }
            }
        }
    }
}

@Composable
fun PostItem(post: PostUI) {
    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)) {
                if (post.status is LoadingStatus.Loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(post.avatarUrl)
                            .crossfade(true)
                            .build(),
                        error = painterResource(R.drawable.ic_error_placeholder),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text = post.name, style = MaterialTheme.typography.titleMedium)
        }

        Text(text = post.body, modifier = Modifier.padding(vertical = 8.dp))

        when (post.status) {
            is LoadingStatus.Loading -> {
                Text("Загрузка комментариев...", style = MaterialTheme.typography.labelSmall)
            }
            is LoadingStatus.Error -> {
                Text("Ошибка загрузки", color = Color.Red)
            }
            is LoadingStatus.Ready -> {
                post.comments.forEach { comment ->
                    Text("${comment.name} • ${comment.body}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}





@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    MainScreen(Modifier.systemBarsPadding())
}
