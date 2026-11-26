package com.youtubevault.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.youtubevault.app.data.DriveRepository
import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.model.VideoSource
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    upNextVideos: List<VideoItem>,
    accessToken: String,
    resumeTimeSec: Long,
    isFavorite: Boolean,
    onSelectVideo: (VideoItem) -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onChannelClick: (() -> Unit)?,
    onRandom: () -> Unit,
    onProgress: (Long, Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullscreen by remember { mutableStateOf(false) }

    // Handle Android Back button: exit fullscreen first before closing player
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // Apply landscape orientation + immersive system bars when fullscreen is active
    DisposableEffect(isFullscreen) {
        val activity = context as? Activity
        if (activity != null) {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { }
    }

    // Always restore orientation and system bars when leaving PlayerScreen
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(activity.window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar (hidden in fullscreen)
            if (!isFullscreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        text = video.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onRandom) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Random", tint = Color.White)
                    }
                    IconButton(onClick = { isFullscreen = true }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                    }
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val activity = context as? Activity
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            activity?.enterPictureInPictureMode(params)
                        }
                    }) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                    }
                }
            }

            // Video Player Area
            Box(
                modifier = if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                },
                contentAlignment = Alignment.Center
            ) {
                if (video.source == VideoSource.DRIVE) {
                    val exoPlayer = remember(video.id, accessToken) {
                        val okHttp = OkHttpClient.Builder()
                            .addInterceptor { chain ->
                                val req = chain.request().newBuilder()
                                    .addHeader("Authorization", "Bearer $accessToken")
                                    .build()
                                chain.proceed(req)
                            }
                            .build()

                        val dataSourceFactory = OkHttpDataSource.Factory(okHttp)
                        ExoPlayer.Builder(context)
                            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                            .build()
                            .apply {
                                setMediaItem(MediaItem.fromUri(DriveRepository.streamUrl(video.id)))
                                prepare()
                                if (resumeTimeSec > 0) seekTo(resumeTimeSec * 1000L)
                                playWhenReady = true
                            }
                    }

                    LaunchedEffect(exoPlayer) {
                        while (true) {
                            delay(3000)
                            val dur = exoPlayer.duration / 1000L
                            val cur = exoPlayer.currentPosition / 1000L
                            if (dur > 0) onProgress(cur, dur)
                        }
                    }

                    DisposableEffect(exoPlayer) {
                        onDispose {
                            val dur = exoPlayer.duration / 1000L
                            val cur = exoPlayer.currentPosition / 1000L
                            if (dur > 0) onProgress(cur, dur)
                            exoPlayer.release()
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                setFullscreenButtonClickListener { full ->
                                    isFullscreen = full
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    var fullscreenCustomView by remember { mutableStateOf<View?>(null) }

                    key(video.id) {
                        AndroidView(
                            factory = { ctx ->
                                val container = FrameLayout(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                                val ytView = YouTubePlayerView(ctx).apply {
                                    enableAutomaticInitialization = false
                                    lifecycleOwner.lifecycle.addObserver(this)

                                    addFullscreenListener(object : FullscreenListener {
                                        override fun onEnterFullscreen(
                                            fullscreenView: View,
                                            exitFullscreen: () -> Unit
                                        ) {
                                            isFullscreen = true
                                            fullscreenCustomView = fullscreenView
                                            container.addView(fullscreenView)
                                        }

                                        override fun onExitFullscreen() {
                                            isFullscreen = false
                                            fullscreenCustomView?.let { container.removeView(it) }
                                            fullscreenCustomView = null
                                        }
                                    })

                                    val options = IFramePlayerOptions.Builder()
                                        .controls(1)
                                        .fullscreen(1)
                                        .build()

                                    initialize(object : AbstractYouTubePlayerListener() {
                                        override fun onReady(youTubePlayer: YouTubePlayer) {
                                            youTubePlayer.loadVideo(video.id, resumeTimeSec.toFloat())
                                        }

                                        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                            val dur = video.durationSec?.toLong() ?: 0L
                                            if (dur > 0) onProgress(second.toLong(), dur)
                                        }
                                    }, options)
                                }
                                container.addView(ytView)
                                container
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Floating Exit Fullscreen button in fullscreen mode
                if (isFullscreen) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }

            // Video Details, Actions & Up Next List (hidden in fullscreen)
            if (!isFullscreen) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = video.channel,
                            color = if (video.channelId != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = if (video.channelId != null && onChannelClick != null) {
                                Modifier.clickable { onChannelClick() }
                            } else Modifier
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFEF4444) else LocalContentColor.current
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isFavorite) "Favourited" else "Favourite")
                            }

                            FilledTonalButton(onClick = onAddToPlaylist) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save")
                            }

                            FilledTonalButton(onClick = { isFullscreen = true }) {
                                Icon(Icons.Default.Fullscreen, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Full")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                    }

                    val nextList = upNextVideos.filter { it.id != video.id }.take(15)
                    if (nextList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Up Next",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(nextList, key = { "next_${it.source}_${it.id}" }) { nextVid ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectVideo(nextVid) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(128.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = nextVid.thumbnail,
                                        contentDescription = nextVid.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    nextVid.duration?.let { dur ->
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = dur,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nextVid.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = nextVid.channel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
