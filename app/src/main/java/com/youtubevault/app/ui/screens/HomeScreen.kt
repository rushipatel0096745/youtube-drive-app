package com.youtubevault.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.youtubevault.app.model.AppThemeMode
import com.youtubevault.app.model.PlaylistFolder
import com.youtubevault.app.model.SortOrder
import com.youtubevault.app.model.SourceFilter
import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.ui.components.BottomNav
import com.youtubevault.app.ui.components.SkeletonVideoCard
import com.youtubevault.app.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userEmail: String,
    videos: List<VideoItem>,
    isLoading: Boolean,
    ytQuotaExceeded: Boolean,
    sourceFilter: SourceFilter,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    sortOrder: SortOrder,
    privateMode: Boolean,
    themeMode: AppThemeMode,
    favorites: Set<String>,
    getProgressFraction: (String) -> Float,
    playlistFolders: List<PlaylistFolder>,
    openedPlaylistName: String?,
    onSourceFilterChange: (SourceFilter) -> Unit,
    onCategorySelect: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onTogglePrivateMode: () -> Unit,
    onToggleTheme: () -> Unit,
    onShuffle: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onPlayVideoFromPlaylist: (VideoItem, String, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenPlaylistModal: (VideoItem) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onCloseOpenedPlaylist: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onOpenChannel: (VideoItem) -> Unit
) {
    var showAccountMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (privateMode) Icons.Default.Lock else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (privateMode) "Private Vault" else "YouTube Vault",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShuffle) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                    }
                    IconButton(onClick = onTogglePrivateMode) {
                        Icon(
                            imageVector = if (privateMode) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Private Mode",
                            tint = if (privateMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme"
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Box {
                        IconButton(onClick = { showAccountMenu = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Account")
                        }
                        DropdownMenu(
                            expanded = showAccountMenu,
                            onDismissRequest = { showAccountMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(userEmail, style = MaterialTheme.typography.bodySmall) },
                                onClick = {}
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Sign Out") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                                onClick = {
                                    showAccountMenu = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomNav(
                selectedFilter = sourceFilter,
                onSelect = onSourceFilterChange
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pill Search & Sort row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search Drive & YouTube...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                trailingIcon = {
                                    if (sortOrder == order) Icon(Icons.Default.Check, contentDescription = null)
                                },
                                onClick = {
                                    onSortChange(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Quota fallback banner
            AnimatedVisibility(visible = ytQuotaExceeded) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "YouTube daily quota reached — showing playlist fallback & Drive vault.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Categories or Playlists bar
            if (sourceFilter == SourceFilter.PLAYLIST) {
                if (openedPlaylistName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCloseOpenedPlaylist) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Playlists")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = openedPlaylistName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${videos.size} videos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (videos.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    onPlayVideoFromPlaylist(videos.first(), openedPlaylistName, videos)
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play All")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            } else if (sourceFilter != SourceFilter.FAVOURITES) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = category == selectedCategory,
                            onClick = { onCategorySelect(category) },
                            label = { Text(category) }
                        )
                    }
                }
            }

            // Content
            if (sourceFilter == SourceFilter.PLAYLIST && openedPlaylistName == null) {
                // Playlist Folders List (YouTube-style)
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Your Playlists",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalButton(onClick = { showCreatePlaylistDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Playlist")
                            }
                        }
                    }

                    items(playlistFolders, key = { "folder_${it.name}" }) { folder ->
                        val firstThumb = folder.videos.firstOrNull { it.thumbnail.isNotBlank() }?.thumbnail
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPlaylist(folder.name) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!firstThumb.isNullOrBlank()) {
                                        AsyncImage(
                                            model = firstThumb,
                                            contentDescription = folder.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when (folder.name) {
                                                "Drive Videos" -> Icons.Default.Folder
                                                "Liked Videos" -> Icons.Default.Favorite
                                                else -> Icons.AutoMirrored.Filled.PlaylistPlay
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.78f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${folder.videos.size}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when {
                                            folder.name == "Drive Videos" -> "Google Drive • ${folder.videos.size} videos"
                                            folder.name == "Liked Videos" -> "Favourites • ${folder.videos.size} videos"
                                            else -> "${folder.videos.size} videos"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!folder.isSystem) {
                                    IconButton(onClick = { onDeletePlaylist(folder.name) }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Playlist",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (isLoading) {
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
                            text = "No videos found",
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
                    items(videos, key = { "${it.source}_${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            isFavorite = favorites.contains(video.id),
                            progressFraction = getProgressFraction(video.id),
                            onPlay = {
                                if (sourceFilter == SourceFilter.PLAYLIST && openedPlaylistName != null) {
                                    onPlayVideoFromPlaylist(video, openedPlaylistName, videos)
                                } else {
                                    onPlayVideo(video)
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onAddToPlaylist = { onOpenPlaylistModal(video) },
                            onChannelClick = { onOpenChannel(video) }
                        )
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistInput,
                    onValueChange = { newPlaylistInput = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistInput.isNotBlank()) {
                            onCreatePlaylist(newPlaylistInput)
                            newPlaylistInput = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }
}
