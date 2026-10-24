package com.animapper.app.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.animapper.app.data.api.models.Episode
import com.animapper.app.data.api.models.MediaDetail
import com.animapper.app.data.local.MyListDao
import com.animapper.app.data.local.MyListItem
import com.animapper.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailState(
    val loading: Boolean = true,
    val detail: MediaDetail? = null,
    val episodes: List<Episode> = emptyList(),
    val provider: String? = null,
    val isInMyList: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: AnimeRepository,
    private val myListDao: MyListDao,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailState())
    val state = _state.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            _state.value = DetailState(loading = true)
            try {
                val d = repo.detail(id)
                val provider = d?.streamingProviders?.firstOrNull()?.name
                val eps = if (provider != null) repo.episodes(id, provider) else emptyList()
                val inList = myListDao.exists(id)
                _state.value = DetailState(false, d, eps, provider, inList)
            } catch (e: Exception) {
                _state.value = DetailState(loading = false, error = e.message)
            }
        }
    }

    fun toggleMyList() {
        val d = _state.value.detail ?: return
        viewModelScope.launch {
            if (_state.value.isInMyList) myListDao.delete(d.id)
            else myListDao.upsert(MyListItem(d.id,
                d.titles["vi"] ?: d.titles["en"] ?: "",
                d.images.coverLg ?: d.images.coverMd))
            _state.value = _state.value.copy(isInMyList = !_state.value.isInMyList)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    id: Int,
    onBack: () -> Unit,
    onPlay: (String, String, String?) -> Unit,
    vm: DetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(id) { vm.load(id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.titles?.get("vi") ?: state.detail?.titles?.get("en") ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = vm::toggleMyList) {
                        Icon(if (state.isInMyList) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "My List")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text("Lỗi: ${state.error}") }
            state.detail != null -> {
                val d = state.detail!!
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        AsyncImage(model = d.images.bannerUrl ?: d.images.coverXl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            contentScale = ContentScale.Crop)
                    }
                    item {
                        Column(Modifier.padding(16.dp)) {
                            Text(d.titles["vi"] ?: d.titles["en"] ?: "",
                                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row {
                                d.format?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                                Spacer(Modifier.width(4.dp))
                                d.seasonYear?.let { AssistChip(onClick = {}, label = { Text(it.toString()) }) }
                                Spacer(Modifier.width(4.dp))
                                d.status?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                            }
                            d.descriptions["vi"]?.let {
                                Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (d.genres.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Thể loại", fontWeight = FontWeight.SemiBold)
                                LazyRow {
                                    items(d.genres) { g ->
                                        AssistChip(onClick = {}, label = { Text(g.name) },
                                            modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (state.episodes.isNotEmpty()) {
                        item {
                            Text("Tập phim (${state.episodes.size}) — ${state.provider}",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(16.dp, 8.dp))
                        }
                        items(state.episodes, key = { it.episodeId }) { ep ->
                            ListItem(
                                headlineContent = { Text("Tập ${ep.episodeNumber}") },
                                supportingContent = ep.server?.let { { Text("Server: $it") } },
                                modifier = Modifier.clickable {
                                    state.provider?.let { p -> onPlay(ep.episodeId, p, ep.server) }
                                },
                            )
                        }
                    } else {
                        item { Text("Không có tập khả dụng", modifier = Modifier.padding(16.dp)) }
                    }
                }
            }
        }
    }
}
