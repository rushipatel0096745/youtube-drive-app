package com.youtubevault.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youtubevault.app.model.VideoItem

@Composable
fun PlaylistModal(
    video: VideoItem,
    playlists: Map<String, List<String>>,
    onToggleVideo: (String, VideoItem) -> Unit,
    onCreatePlaylist: (String, VideoItem) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (playlists.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(playlists.keys.toList()) { name ->
                            val isPresent = playlists[name]?.contains(video.id) == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleVideo(name, video) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isPresent,
                                    onCheckedChange = { onToggleVideo(name, video) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, modifier = Modifier.weight(1f))
                                Text(
                                    text = "${playlists[name]?.size ?: 0}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New playlist name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreatePlaylist(newName, video)
                                newName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
