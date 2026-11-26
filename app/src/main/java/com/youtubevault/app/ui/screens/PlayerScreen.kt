package com.youtubevault.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.youtubevault.app.BuildConfig
import com.youtubevault.app.data.DriveRepository
import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.model.VideoSource
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    upNextTitle: String = "Up Next",
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
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                    // Direct WebView YouTube Embed Player (reliable playback + native HTML5 fullscreen)
                    var webViewRef by remember(video.id) { mutableStateOf<WebView?>(null) }

                    DisposableEffect(video.id) {
                        onDispose {
                            webViewRef?.onPause()
                            webViewRef?.destroy()
                            webViewRef = null
                        }
                    }

                    key(video.id) {
                        AndroidView(
                            factory = { ctx ->
                                val container = FrameLayout(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                }
                                var customView: View? = null
                                var customCallback: WebChromeClient.CustomViewCallback? = null

                                val webView = WebView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        userAgentString = userAgentString.replace("; wv", "")
                                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val url = request?.url?.toString() ?: return false
                                            // Keep embed / auth / player frames inside WebView; open watch links externally
                                            if (url.contains("youtube.com/watch") || url.contains("youtu.be/")) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                ctx.startActivity(intent)
                                                return true
                                            }
                                            return false
                                        }
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                            if (view == null) return
                                            isFullscreen = true
                                            customView = view
                                            customCallback = callback
                                            container.addView(
                                                view,
                                                FrameLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            )
                                        }

                                        override fun onHideCustomView() {
                                            isFullscreen = false
                                            customView?.let { container.removeView(it) }
                                            customView = null
                                            customCallback?.onCustomViewHidden()
                                            customCallback = null
                                        }
                                    }

                                    val startParam = if (resumeTimeSec > 0) "&start=$resumeTimeSec" else ""
                                    val html = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                          <style>
                                            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
                                            iframe { width: 100%; height: 100%; border: 0; }
                                          </style>
                                        </head>
                                        <body>
                                          <iframe
                                            src="https://www.youtube.com/embed/${video.id}?autoplay=1&playsinline=1&fs=1&rel=0&enablejsapi=1$startParam"
                                            referrerpolicy="strict-origin-when-cross-origin"
                                            allow="autoplay; encrypted-media; fullscreen; picture-in-picture"
                                            allowfullscreen>
                                          </iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    loadDataWithBaseURL(
                                        "https://${BuildConfig.APPLICATION_ID}",
                                        html,
                                        "text/html",
                                        "utf-8",
                                        null
                                    )
                                }
                                webViewRef = webView
                                container.addView(webView)
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
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = if (onChannelClick != null) {
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onChannelClick() }
                                    .padding(vertical = 2.dp)
                            } else Modifier
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFEF4444) else LocalContentColor.current,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isFavorite) "Favourited" else "Fav")
                            }

                            FilledTonalButton(onClick = onAddToPlaylist) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }

                            FilledTonalButton(onClick = { isFullscreen = true }) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Full")
                            }

                            if (video.source == VideoSource.YOUTUBE) {
                                IconButton(onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/watch?v=${video.id}")
                                    )
                                    context.startActivity(intent)
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open in YouTube"
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                    }

                    val nextList = upNextVideos.filter { it.id != video.id }.take(15)
                    if (nextList.isNotEmpty()) {
                        item {
                            Text(
                                text = upNextTitle,
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
