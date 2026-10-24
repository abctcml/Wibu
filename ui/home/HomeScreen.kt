package com.animapper.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.animapper.app.data.api.models.SearchResult
import com.animapper.app.data.local.MyListItem
import com.animapper.app.data.local.WatchHistory
import com.animapper.app.data.local.MyListDao
import com.animapper.app.data.local.WatchHistoryDao
import com.animapper.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeState(
    val loading: Boolean = true,
    val trending: List<SearchResult> = emptyList(),
    val seasonal: List<SearchResult> = emptyList(),
    val topRated: List<SearchResult> = emptyList(),
    val japanese: List<SearchResult> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: AnimeRepository,
    historyDao: WatchHistoryDao,
    myListDao: MyListDao,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()
    val history: StateFlow<List<WatchHistory>> = historyDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val myList: StateFlow<List<MyListItem>> = myListDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = HomeState(loading = true)
            try {
                val cal = Calendar.getInstance()
                val season = when (cal.get(Calendar.MONTH)) {
                    in 0..2 -> "WINTER"; in 3..5 -> "SPRING"; in 6..8 -> "SUMMER"; else -> "FALL"
                }
                val year = cal.get(Calendar.YEAR)
                val t = async { repo.trending() }
                val s = async { repo.seasonal(season, year) }
                val tr = async { repo.topRated() }
                val jp = async { repo.byCountry("JP") }
                _state.value = HomeState(
                    loading = false,
                    trending = t.await(), seasonal = s.await(),
                    topRated = tr.await(), japanese = jp.await(),
                )
            } catch (e: Exception) {
                _state.value = HomeState(loading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDetail: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val history by vm.history.collectAsState()
    val myList by vm.myList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AniMapper", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, "Search") }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lỗi: ${state.error}")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = vm::refresh) { Text("Thử lại") }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)) {
                if (history.isNotEmpty()) {
                    item { SectionTitle("Tiếp tục xem") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(history, key = { it.mediaId }) { h ->
                                MediaCard(h.title, h.coverUrl) { onOpenDetail(h.mediaId) }
                            }
                        }
                    }
                }
                if (myList.isNotEmpty()) {
                    item { SectionTitle("Danh sách của tôi") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(myList, key = { it.mediaId }) { m ->
                                MediaCard(m.title, m.coverUrl) { onOpenDetail(m.mediaId) }
                            }
                        }
                    }
                }
                item { SectionTitle("Thịnh hành"); MediaRow(state.trending, onOpenDetail) }
                item { SectionTitle("Mùa này"); MediaRow(state.seasonal, onOpenDetail) }
                item { SectionTitle("Đánh giá cao"); MediaRow(state.topRated, onOpenDetail) }
                item { SectionTitle("Anime Nhật Bản"); MediaRow(state.japanese, onOpenDetail) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp))
}

@Composable
private fun MediaRow(items: List<SearchResult>, onClick: (Int) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(items, key = { it.id }) { m ->
            MediaCard(
                title = m.titles["vi"] ?: m.titles["en"] ?: m.titles.values.firstOrNull() ?: "?",
                cover = m.images.coverLg ?: m.images.coverMd,
            ) { onClick(m.id) }
        }
    }
}

@Composable
private fun MediaCard(title: String, cover: String?, onClick: () -> Unit) {
    Card(modifier = Modifier.padding(end = 8.dp).width(140.dp).clickable(onClick = onClick)) {
        Column {
            AsyncImage(model = cover, contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop)
            Text(title, maxLines = 2, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(6.dp))
        }
    }
}
