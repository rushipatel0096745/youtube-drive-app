package com.youtubevault.app.data

import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

data class DriveLoadResult(
    val folderId: String?,
    val driveVideos: List<VideoItem>,
    val linkedYouTubeIds: List<String>
)

class DriveRepository(private val client: OkHttpClient = OkHttpClient()) {

    suspend fun loadDriveVault(accessToken: String): DriveLoadResult = withContext(Dispatchers.IO) {
        val folderId = findVideosFolderId(accessToken)
            ?: return@withContext DriveLoadResult(null, emptyList(), emptyList())

        val q = URLEncoder.encode("'$folderId' in parents and trashed = false", "UTF-8")
        val fields = URLEncoder.encode(
            "files(id,name,mimeType,thumbnailLink,videoMediaMetadata(durationMillis))",
            "UTF-8"
        )
        val url = "https://www.googleapis.com/drive/v3/files?q=$q&fields=$fields&pageSize=200"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return@withContext DriveLoadResult(folderId, emptyList(), emptyList())
        }

        val body = response.body?.string().orEmpty()
        val json = JSONObject(body)
        val files = json.optJSONArray("files") ?: return@withContext DriveLoadResult(
            folderId, emptyList(), emptyList()
        )

        val videos = mutableListOf<VideoItem>()
        var linksFileId: String? = null

        for (i in 0 until files.length()) {
            val file = files.getJSONObject(i)
            val id = file.optString("id")
            val name = file.optString("name")
            val mimeType = file.optString("mimeType")

            if (name.equals("links.txt", ignoreCase = true)) {
                linksFileId = id
                continue
            }

            if (mimeType.startsWith("video/")) {
                val meta = file.optJSONObject("videoMediaMetadata")
                val durationMs = meta?.optLong("durationMillis", 0L) ?: 0L
                val durationSec = if (durationMs > 0) (durationMs / 1000).toInt() else null
                val formattedDuration = durationSec?.let { formatSeconds(it) }

                videos.add(
                    VideoItem(
                        id = id,
                        source = VideoSource.DRIVE,
                        title = name.substringBeforeLast("."),
                        category = "Drive",
                        thumbnail = file.optString("thumbnailLink", ""),
                        duration = formattedDuration,
                        durationSec = durationSec,
                        channel = "Google Drive"
                    )
                )
            }
        }

        val linkedIds = if (linksFileId != null) {
            fetchYouTubeLinksFromFile(accessToken, linksFileId)
        } else {
            emptyList()
        }

        DriveLoadResult(folderId, videos, linkedIds)
    }

    private fun findVideosFolderId(accessToken: String): String? {
        val q = URLEncoder.encode(
            "name = 'videos' and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
            "UTF-8"
        )
        val url = "https://www.googleapis.com/drive/v3/files?q=$q&fields=files(id,name)"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val json = JSONObject(response.body?.string().orEmpty())
        val files = json.optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).optString("id") else null
    }

    private fun fetchYouTubeLinksFromFile(accessToken: String, fileId: String): List<String> {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val content = response.body?.string().orEmpty()

        val regex = Regex("""(?:v=|youtu\.be/|shorts/|embed/)([a-zA-Z0-9_-]{11})""")
        return content.lines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) null
                else regex.find(trimmed)?.groupValues?.get(1)
                    ?: if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) trimmed else null
            }
            .distinct()
    }

    companion object {
        fun streamUrl(fileId: String): String =
            "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"

        fun formatSeconds(totalSeconds: Int): String {
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
            else String.format("%d:%02d", m, s)
        }
    }
}
