package com.youtubevault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.youtubevault.app.model.AppThemeMode
import com.youtubevault.app.model.SortOrder
import com.youtubevault.app.model.SourceFilter
import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.ui.components.BottomNav
import com.youtubevault.app.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userEmail: String,
    videos: List<VideoItem>,
    isLoading: Boolean,
    sourceFilter: SourceFilter,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    sortOrder: SortOrder,
    privateMode: Boolean,
    themeMode: AppThemeMode,
    favorites: Set<String>,
    playlists: Map<String, List<String>>,
    selectedPlaylistName: String?,
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
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenPlaylistModal: (VideoItem) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onOpenChannel: (VideoItem) -> Unit
) {
    var showAccountMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (privateMode) "Vault" else "YouTube Vault",
                            fontWeight = FontWeight.Bold
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
                                leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
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
            // Search & Sort row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
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

            // Categories or Playlists bar
            if (sourceFilter == SourceFilter.PLAYLIST) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists.keys.toList()) { plName ->
                        FilterChip(
                            selected = plName == selectedPlaylistName,
                            onClick = { onSelectPlaylist(plName) },
                            label = { Text("$plName (${playlists[plName]?.size ?: 0})") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onDeletePlaylist(plName) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No videos found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { "${it.source}_${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            isFavorite = favorites.contains(video.id),
                            onPlay = { onPlayVideo(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onAddToPlaylist = { onOpenPlaylistModal(video) },
                            onChannelClick = if (video.channelId != null) {
                                { onOpenChannel(video) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
