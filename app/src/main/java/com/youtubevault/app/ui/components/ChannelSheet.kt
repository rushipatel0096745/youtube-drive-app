package com.youtubevault.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youtubevault.app.model.VideoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSheet(
    channelName: String,
    videos: List<VideoItem>,
    isLoading: Boolean,
    favorites: Set<String>,
    onPlay: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onAddToPlaylist: (VideoItem) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = channelName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 460.dp)
                ) {
                    items(videos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            isFavorite = favorites.contains(video.id),
                            onPlay = {
                                onDismiss()
                                onPlay(video)
                            },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onAddToPlaylist = { onAddToPlaylist(video) }
                        )
                    }
                }
            }
        }
    }
}
