package com.youtubevault.app.data

import com.youtubevault.app.BuildConfig
import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

data class YouTubeFetchResult(
    val videos: List<VideoItem>,
    val quotaExceeded: Boolean = false
)

class YouTubeRepository(private val client: OkHttpClient = OkHttpClient()) {

    private val apiKey: String get() = BuildConfig.YOUTUBE_API_KEY

    companion object {
        val NORMAL_CATEGORIES = listOf("All", "Trending", "Music", "Gaming", "Tech", "News", "Podcasts")
        val PRIVATE_CATEGORIES = listOf("All", "Short", "Long", "Undercut", "Colors")

        val NORMAL_CATEGORY_IDS = mapOf(
            "All" to null,
            "Trending" to "0",
            "Music" to "10",
            "Gaming" to "20",
            "Tech" to "28",
            "News" to "25",
            "Podcasts" to "22"
        )

        val CULTURE_CAFE_HAIRCUT_VIDS = listOf(
            "jol6Vp8wlz8",
            "RaObgu0xoZA",
            "Ta4_RFmVQro"
        )

        val PRIVATE_CATEGORY_PLAYLISTS = mapOf(
            "All" to listOf(
                "UUyxIgscUAeonLzrnMIcVV0A",
                "UUgRIzrQLT9FUjl9QT1Jq9aA",
                "PLW77CFOjU1MEpngokZElo00dXvtZ-1Ml5",
                "UU8HyecFpFcW5NfJTJjrftWw",
                "UUCFFT7bZr09Tqo65vu5zPiQ",
                "UUDIqXqP9PL9fNH7CfbGeFsQ",
                "UUyyAQ8440d8C-0UF-pD0v8w",
                "UUG9thQ9Ce-VSL8gHFSy1M_w"
            ),
            "Short" to listOf(
                "PLW77CFOjU1MEpngokZElo00dXvtZ-1Ml5",
                "UUCFFT7bZr09Tqo65vu5zPiQ",
                "UUyxIgscUAeonLzrnMIcVV0A",
                "UUgRIzrQLT9FUjl9QT1Jq9aA"
            ),
            "Long" to listOf(
                "PLW77CFOjU1MEpngokZElo00dXvtZ-1Ml5",
                "UU8HyecFpFcW5NfJTJjrftWw",
                "UUgRIzrQLT9FUjl9QT1Jq9aA",
                "UUyxIgscUAeonLzrnMIcVV0A"
            ),
            "Undercut" to listOf(
                "UUyxIgscUAeonLzrnMIcVV0A",
                "UUgRIzrQLT9FUjl9QT1Jq9aA",
                "UU8HyecFpFcW5NfJTJjrftWw",
                "PLW77CFOjU1MEpngokZElo00dXvtZ-1Ml5"
            ),
            "Colors" to listOf(
                "UUjzof44RG883SW61XlkP-Pg",
                "PLW77CFOjU1MEpngokZElo00dXvtZ-1Ml5",
                "UUG9thQ9Ce-VSL8gHFSy1M_w",
                "UUyxIgscUAeonLzrnMIcVV0A"
            )
        )
    }

    suspend fun fetchVideosByIds(ids: List<String>, category: String = "YouTube"): List<VideoItem> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || ids.isEmpty()) return@withContext emptyList()
            val results = mutableListOf<VideoItem>()
            ids.chunked(50).forEach { chunk ->
                val joined = chunk.joinToString(",")
                val url =
                    "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=$joined&key=$apiKey"
                val (json, _) = getJsonWithQuotaCheck(url)
                val items = json?.optJSONArray("items") ?: return@forEach
                for (i in 0 until items.length()) {
                    parseVideoResource(items.getJSONObject(i), category)?.let { results.add(it) }
                }
            }
            results
        }

    suspend fun fetchPopularVideos(category: String): YouTubeFetchResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext YouTubeFetchResult(emptyList())
        val catId = NORMAL_CATEGORY_IDS[category]
        val catParam = if (!catId.isNullOrEmpty() && catId != "0") "&videoCategoryId=$catId" else ""
        val url =
            "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&chart=mostPopular&regionCode=IN&maxResults=25$catParam&key=$apiKey"
        val (json, quotaExceeded) = getJsonWithQuotaCheck(url)
        if (quotaExceeded || json == null) {
            return@withContext YouTubeFetchResult(emptyList(), quotaExceeded)
        }
        val items = json.optJSONArray("items") ?: return@withContext YouTubeFetchResult(emptyList())
        val list = mutableListOf<VideoItem>()
        for (i in 0 until items.length()) {
            parseVideoResource(items.getJSONObject(i), category)?.let { list.add(it) }
        }
        YouTubeFetchResult(list, false)
    }

    suspend fun fetchPlaylistVideos(playlistId: String, category: String, maxResults: Int = 12): YouTubeFetchResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext YouTubeFetchResult(emptyList())
            val url =
                "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&playlistId=$playlistId&maxResults=$maxResults&key=$apiKey"
            val (json, quotaExceeded) = getJsonWithQuotaCheck(url)
            if (quotaExceeded || json == null) {
                return@withContext YouTubeFetchResult(emptyList(), quotaExceeded)
            }
            val items = json.optJSONArray("items") ?: return@withContext YouTubeFetchResult(emptyList())
            val videoIds = mutableListOf<String>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val vid = item.optJSONObject("contentDetails")?.optString("videoId")
                    ?: item.optJSONObject("snippet")?.optJSONObject("resourceId")?.optString("videoId")
                if (!vid.isNullOrBlank()) videoIds.add(vid)
            }
            YouTubeFetchResult(fetchVideosByIds(videoIds, category), false)
        }

    /**
     * 1-unit playlist fallback for Private Mode when search/quota is exhausted.
     * Rotates creators by refreshCount and interleaves results.
     */
    suspend fun fetchPrivateMixedFallback(category: String, refreshCount: Int): YouTubeFetchResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext YouTubeFetchResult(emptyList())
            val plList = PRIVATE_CATEGORY_PLAYLISTS[category] ?: PRIVATE_CATEGORY_PLAYLISTS["All"].orEmpty()
            if (plList.isEmpty()) return@withContext YouTubeFetchResult(emptyList())

            val rotateOffset = (refreshCount * 2) % plList.size
            val rotated = plList.drop(rotateOffset) + plList.take(rotateOffset)
            val selected = rotated.take(5)

            val allLists = mutableListOf<List<VideoItem>>()
            var anyQuota = false
            for (plId in selected) {
                val res = fetchPlaylistVideos(plId, category, maxResults = 8)
                if (res.quotaExceeded) anyQuota = true
                if (res.videos.isNotEmpty()) allLists.add(res.videos)
            }

            val ccVideos = fetchVideosByIds(CULTURE_CAFE_HAIRCUT_VIDS, category)

            val mixed = mutableListOf<VideoItem>()
            val maxLen = (allLists.maxOfOrNull { it.size } ?: 0).coerceAtLeast(ccVideos.size)
            for (i in 0 until maxLen) {
                if (i < ccVideos.size) mixed.add(ccVideos[i])
                for (list in allLists) {
                    if (i < list.size) mixed.add(list[i])
                }
            }

            YouTubeFetchResult(mixed.distinctBy { it.id }, anyQuota && mixed.isEmpty())
        }

    suspend fun searchVideos(query: String): YouTubeFetchResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || query.isBlank()) return@withContext YouTubeFetchResult(emptyList())
        val q = URLEncoder.encode(query, "UTF-8")
        val url =
            "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoEmbeddable=true&maxResults=24&q=$q&key=$apiKey"
        val (json, quotaExceeded) = getJsonWithQuotaCheck(url)
        if (quotaExceeded || json == null) {
            return@withContext YouTubeFetchResult(emptyList(), quotaExceeded)
        }
        val items = json.optJSONArray("items") ?: return@withContext YouTubeFetchResult(emptyList())
        val ids = mutableListOf<String>()
        for (i in 0 until items.length()) {
            val vid = items.getJSONObject(i).optJSONObject("id")?.optString("videoId")
            if (!vid.isNullOrBlank()) ids.add(vid)
        }
        YouTubeFetchResult(fetchVideosByIds(ids, "Search"), false)
    }

    suspend fun fetchChannelVideos(channelId: String): List<VideoItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || channelId.isBlank()) return@withContext emptyList()
        val uploadsPlaylist = if (channelId.startsWith("UC")) "UU" + channelId.drop(2) else channelId
        fetchPlaylistVideos(uploadsPlaylist, "Channel", maxResults = 15).videos
    }

    private fun getJsonWithQuotaCheck(url: String): Pair<JSONObject?, Boolean> {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isNotBlank()) JSONObject(body) else null
            val error = json?.optJSONObject("error")
            val code = error?.optInt("code", response.code) ?: response.code
            val status = error?.optString("status", "") ?: ""
            val reason = error?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason", "") ?: ""

            val isQuota = code == 403 ||
                code == 429 ||
                status == "RESOURCE_EXHAUSTED" ||
                reason == "quotaExceeded" ||
                reason == "rateLimitExceeded" ||
                reason == "dailyLimitExceeded"

            if (!response.isSuccessful || isQuota) {
                null to isQuota
            } else {
                json to false
            }
        } catch (_: Exception) {
            null to false
        }
    }

    private fun parseVideoResource(obj: JSONObject, category: String): VideoItem? {
        val id = obj.optString("id")
        if (id.isBlank()) return null
        val snippet = obj.optJSONObject("snippet") ?: return null
        val title = snippet.optString("title")
        val channel = snippet.optString("channelTitle", "YouTube")
        val channelId = snippet.optString("channelId", "")
        val thumbs = snippet.optJSONObject("thumbnails")
        val thumbUrl = thumbs?.optJSONObject("high")?.optString("url")
            ?: thumbs?.optJSONObject("medium")?.optString("url")
            ?: thumbs?.optJSONObject("default")?.optString("url")
            ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"

        val isoDuration = obj.optJSONObject("contentDetails")?.optString("duration")
        val sec = isoDuration?.let { parseIsoDuration(it) }

        return VideoItem(
            id = id,
            source = VideoSource.YOUTUBE,
            title = title,
            category = category,
            thumbnail = thumbUrl,
            duration = sec?.let { DriveRepository.formatSeconds(it) },
            durationSec = sec,
            channel = channel,
            channelId = channelId.ifBlank { null }
        )
    }

    private fun parseIsoDuration(iso: String): Int? {
        val regex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")
        val match = regex.matchEntire(iso) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: 0
        val m = match.groupValues[2].toIntOrNull() ?: 0
        val s = match.groupValues[3].toIntOrNull() ?: 0
        val total = h * 3600 + m * 60 + s
        return if (total > 0) total else null
    }
}
