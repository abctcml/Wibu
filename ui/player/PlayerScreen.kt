package com.animapper.app.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.animapper.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerState(
    val loading: Boolean = true,
    val streamUrl: String? = null,
    val isEmbed: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(private val repo: AnimeRepository) : ViewModel() {
    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    fun load(episodeId: String, provider: String, server: String?) {
        viewModelScope.launch {
            _state.value = PlayerState(loading = true)
            try {
                val src = repo.source(episodeId, provider, server)
                if (src == null) { _state.value = PlayerState(false, error = "Không lấy được nguồn"); return@launch }
                _state.value = PlayerState(false, src.url, src.type == "EMBED")
            } catch (e: Exception) {
                _state.value = PlayerState(false, error = e.message)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    episodeId: String, provider: String, server: String?,
    onBack: () -> Unit,
    vm: PlayerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(episodeId, provider, server) { vm.load(episodeId, provider, server) }

    val ctx = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    LaunchedEffect(state.streamUrl) {
        val url = state.streamUrl
        if (url != null && !state.isEmbed) {
            val ds = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0")
                .setAllowCrossProtocolRedirects(true)
            exoPlayer.setMediaSource(HlsMediaSource.Factory(ds).createMediaSource(MediaItem.fromUri(url)))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            state.error != null -> Text(state.error!!, color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(16.dp))
            state.isEmbed -> Text("Nguồn EMBED chưa hỗ trợ. Vui lòng chọn server khác.",
                color = Color.White, modifier = Modifier.align(Alignment.Center).padding(16.dp))
            state.streamUrl != null -> AndroidView(
                factory = { c -> PlayerView(c).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                } },
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
        }
    }
}
