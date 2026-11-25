package com.youtubevault.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.youtubevault.app.model.SourceFilter

@Composable
fun BottomNav(
    selectedFilter: SourceFilter,
    onSelect: (SourceFilter) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val items = listOf(
            SourceFilter.BLENDED to Icons.Default.Home,
            SourceFilter.DRIVE to Icons.Default.Cloud,
            SourceFilter.YOUTUBE to Icons.Default.PlayArrow,
            SourceFilter.FAVOURITES to Icons.Default.Favorite,
            SourceFilter.PLAYLIST to Icons.Default.QueueMusic
        )
        items.forEach { (filter, icon) ->
            NavigationBarItem(
                selected = selectedFilter == filter,
                onClick = { onSelect(filter) },
                icon = { Icon(imageVector = icon, contentDescription = filter.label) },
                label = { Text(filter.label) }
            )
        }
    }
}
