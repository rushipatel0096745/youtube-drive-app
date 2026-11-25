package com.youtubevault.app.model

enum class VideoSource {
    DRIVE,
    YOUTUBE
}

data class VideoItem(
    val id: String,
    val source: VideoSource,
    val title: String,
    val category: String,
    val thumbnail: String,
    val duration: String? = null,
    val durationSec: Int? = null,
    val channel: String,
    val channelId: String? = null
)

enum class SourceFilter(val label: String) {
    BLENDED("All"),
    DRIVE("Drive"),
    YOUTUBE("YouTube"),
    FAVOURITES("Favourites"),
    PLAYLIST("Playlists")
}

enum class SortOrder(val label: String) {
    DEFAULT("Default"),
    AZ("A - Z"),
    DURATION("Duration")
}

enum class AppThemeMode {
    DARK,
    LIGHT
}

data class WatchProgress(
    val currentTime: Long,
    val duration: Long,
    val updatedAt: Long
)
