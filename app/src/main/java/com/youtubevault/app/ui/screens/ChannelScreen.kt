package com.youtubevault.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.youtubevault.app.model.ChannelInfo
import com.youtubevault.app.model.ChannelMeta
import com.youtubevault.app.model.ChannelTab
import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.model.VideoSource
import com.youtubevault.app.ui.components.SkeletonVideoCard
import com.youtubevault.app.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    channelInfo: ChannelInfo,
    channelMeta: ChannelMeta?,
    activeTab: ChannelTab,
    videos: List<VideoItem>,
    isLoading: Boolean,
    favorites: Set<String>,
    onTabChange: (ChannelTab) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenPlaylistModal: (VideoItem) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = channelMeta?.title ?: channelInfo.channelName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Channel Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!channelMeta?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channelMeta?.avatarUrl,
                        contentDescription = channelInfo.channelName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                if (channelInfo.source == VideoSource.DRIVE) Color(0xFF2563EB).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (channelInfo.source == VideoSource.DRIVE) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Text(
                                text = channelInfo.channelName.firstOrNull()?.uppercase() ?: "C",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channelMeta?.title ?: channelInfo.channelName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val statsParts = buildList {
                        channelMeta?.customUrl?.let { add(it) }
                        channelMeta?.subscriberCount?.let { add("$it subscribers") }
                        channelMeta?.videoCount?.let { add("$it videos") }
                    }
                    if (statsParts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statsParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    channelMeta?.description?.let { desc ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tabs: Videos / Popular / Shorts
            if (channelInfo.source == VideoSource.YOUTUBE) {
                TabRow(selectedTabIndex = ChannelTab.entries.indexOf(activeTab)) {
                    ChannelTab.entries.forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { onTabChange(tab) },
                            text = { Text(tab.label, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            } else {
                HorizontalDivider()
            }

            // Videos List
            if (isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(4) { SkeletonVideoCard() }
                }
            } else if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No videos found in this tab",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { "ch_${it.source}_${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            isFavorite = favorites.contains(video.id),
                            onPlay = { onPlayVideo(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onAddToPlaylist = { onOpenPlaylistModal(video) }
                        )
                    }
                }
            }
        }
    }
}
