package com.animapper.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.animapper.app.data.api.models.SearchResult
import com.animapper.app.data.api.models.Suggestion
import com.animapper.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val suggestions: List<Suggestion> = emptyList(),
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val filterYear: Int? = null,
    val filterCountry: String? = null,
    val filterFormat: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(private val repo: AnimeRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()
    private var suggestJob: Job? = null

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
        suggestJob?.cancel()
        if (q.isBlank()) { _state.value = _state.value.copy(suggestions = emptyList()); return }
        suggestJob = viewModelScope.launch {
            delay(300)
            runCatching { repo.autocomplete(q) }
                .onSuccess { _state.value = _state.value.copy(suggestions = it) }
        }
    }

    fun search() {
        val q = _state.value.query
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, suggestions = emptyList())
            runCatching { repo.search(q) }
                .onSuccess { _state.value = _state.value.copy(results = it, loading = false) }
                .onFailure { _state.value = _state.value.copy(loading = false) }
        }
    }

    fun applyFilter(year: Int?, country: String?, format: String?) {
        _state.value = _state.value.copy(filterYear = year, filterCountry = country, filterFormat = format)
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { repo.filter(format = format, seasonYear = year, countryOfOrigin = country) }
                .onSuccess { _state.value = _state.value.copy(results = it, loading = false) }
                .onFailure { _state.value = _state.value.copy(loading = false) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showFilter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query, onValueChange = vm::onQueryChange,
                        placeholder = { Text("Tìm anime...") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.search() }),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = { TextButton(onClick = { showFilter = !showFilter }) { Text("Lọc") } },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showFilter) {
                FilterRow(state.filterYear, state.filterCountry, state.filterFormat, vm::applyFilter)
            }
            when {
                state.suggestions.isNotEmpty() -> LazyColumn {
                    items(state.suggestions.size) { i ->
                        val s = state.suggestions[i]
                        ListItem(
                            headlineContent = { Text(s.title) },
                            modifier = Modifier.clickable {
                                vm.onQueryChange(s.title); vm.search()
                            },
                        )
                    }
                }
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.results, key = { it.id }) { r ->
                        Column(Modifier.clickable { onOpenDetail(r.id) }) {
                            AsyncImage(model = r.images.coverLg ?: r.images.coverMd,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.7f),
                                contentScale = ContentScale.Crop)
                            Text(r.titles["vi"] ?: r.titles["en"] ?: "",
                                maxLines = 2, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    currentYear: Int?, currentCountry: String?, currentFormat: String?,
    onApply: (Int?, String?, String?) -> Unit,
) {
    var year by remember { mutableStateOf(currentYear) }
    var country by remember { mutableStateOf(currentCountry) }
    var format by remember { mutableStateOf(currentFormat) }
    Column(Modifier.padding(8.dp)) {
        Text("Năm", style = MaterialTheme.typography.labelLarge)
        LazyRow {
            items(10) { i ->
                val y = 2024 - i
                FilterChip(selected = year == y, onClick = { year = if (year == y) null else y },
                    label = { Text(y.toString()) }, modifier = Modifier.padding(2.dp))
            }
        }
        Text("Quốc gia", style = MaterialTheme.typography.labelLarge)
        LazyRow {
            listOf("JP" to "Nhật", "KR" to "Hàn", "CN" to "Trung").forEach { (c, n) ->
                FilterChip(selected = country == c, onClick = { country = if (country == c) null else c },
                    label = { Text(n) }, modifier = Modifier.padding(2.dp))
            }
        }
        Text("Format", style = MaterialTheme.typography.labelLarge)
        LazyRow {
            listOf("TV", "MOVIE", "OVA", "ONA").forEach { f ->
                FilterChip(selected = format == f, onClick = { format = if (format == f) null else f },
                    label = { Text(f) }, modifier = Modifier.padding(2.dp))
            }
        }
        Button(onClick = { onApply(year, country, format) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) { Text("Áp dụng") }
    }
}
