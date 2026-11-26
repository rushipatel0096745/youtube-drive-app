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
    val channelId: String? = null,
    val viewCount: Long = 0L
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

enum class ChannelTab(val label: String) {
    ALL("Videos"),
    POPULAR("Popular"),
    SHORTS("Shorts")
}

data class ChannelInfo(
    val channelName: String,
    val channelId: String?,
    val source: VideoSource
)

data class ChannelMeta(
    val channelId: String,
    val title: String,
    val avatarUrl: String?,
    val subscriberCount: String?,
    val videoCount: String?,
    val description: String?,
    val customUrl: String?,
    val uploadsPlaylistId: String?
)

data class PlaylistFolder(
    val name: String,
    val videos: List<VideoItem>,
    val isSystem: Boolean = false
)
