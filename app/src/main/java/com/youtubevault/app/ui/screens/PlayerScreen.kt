package com.youtubevault.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
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
    accessToken: String,
    resumeTimeSec: Long,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onChannelClick: (() -> Unit)?,
    onRandom: () -> Unit,
    onProgress: (Long, Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRandom) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Random", tint = Color.White)
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

            // Video Player Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
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
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            YouTubePlayerView(ctx).apply {
                                lifecycleOwner.lifecycle.addObserver(this)
                                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                    override fun onReady(youTubePlayer: YouTubePlayer) {
                                        youTubePlayer.loadVideo(video.id, resumeTimeSec.toFloat())
                                    }

                                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                        val dur = video.durationSec?.toLong() ?: 0L
                                        if (dur > 0) onProgress(second.toLong(), dur)
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Video Details & Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = video.channel,
                    color = if (video.channelId != null) MaterialTheme.colorScheme.primary else Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = if (video.channelId != null && onChannelClick != null) {
                        Modifier.clickable { onChannelClick() }
                    } else Modifier
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFEF4444) else Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFavorite) "Favourited" else "Favourite", color = Color.White)
                    }

                    OutlinedButton(onClick = onAddToPlaylist) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}
