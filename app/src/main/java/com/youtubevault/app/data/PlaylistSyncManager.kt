package com.youtubevault.app.data

import android.content.Context
import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class PlaylistSyncManager(context: Context, private val client: OkHttpClient = OkHttpClient()) {

    private val prefs = context.getSharedPreferences("yt_vault_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_VIDEO_DETAILS = "video_details"
        private const val KEY_FILE_ID = "playlists_file_id"
        private const val KEY_FOLDER_ID = "folder_id"
    }

    fun setFolderId(folderId: String) {
        if (folderId.isNotBlank()) {
            prefs.edit().putString(KEY_FOLDER_ID, folderId).apply()
        }
    }

    fun loadLocalFavorites(): Set<String> {
        return try {
            val arr = JSONArray(prefs.getString(KEY_FAVORITES, "[]") ?: "[]")
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun loadLocalPlaylists(): Map<String, List<String>> {
        return try {
            val obj = JSONObject(prefs.getString(KEY_PLAYLISTS, "{}") ?: "{}")
            val map = mutableMapOf<String, List<String>>()
            obj.keys().forEach { key ->
                val arr = obj.getJSONArray(key)
                map[key] = (0 until arr.length()).map { arr.getString(it) }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun loadLocalVideoDetails(): Map<String, VideoItem> {
        return try {
            val obj = JSONObject(prefs.getString(KEY_VIDEO_DETAILS, "{}") ?: "{}")
            val map = mutableMapOf<String, VideoItem>()
            obj.keys().forEach { key ->
                val v = obj.getJSONObject(key)
                map[key] = VideoItem(
                    id = v.optString("id", key),
                    source = if (v.optString("source") == "drive") VideoSource.DRIVE else VideoSource.YOUTUBE,
                    title = v.optString("title", ""),
                    category = v.optString("category", ""),
                    thumbnail = v.optString("thumbnail", ""),
                    duration = v.optString("duration").ifBlank { null },
                    durationSec = if (v.has("durationSec") && !v.isNull("durationSec")) v.optInt("durationSec") else null,
                    channel = v.optString("channel", ""),
                    channelId = v.optString("channelId").ifBlank { null }
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveLocal(
        favorites: Set<String>,
        playlists: Map<String, List<String>>,
        videoDetails: Map<String, VideoItem>
    ) {
        val favArr = JSONArray(favorites.toList())
        val plObj = JSONObject()
        playlists.forEach { (name, ids) -> plObj.put(name, JSONArray(ids)) }
        val detailsObj = JSONObject()
        videoDetails.forEach { (id, v) ->
            detailsObj.put(id, JSONObject().apply {
                put("id", v.id)
                put("source", if (v.source == VideoSource.DRIVE) "drive" else "youtube")
                put("title", v.title)
                put("category", v.category)
                put("thumbnail", v.thumbnail)
                if (v.duration != null) put("duration", v.duration)
                if (v.durationSec != null) put("durationSec", v.durationSec)
                put("channel", v.channel)
                if (v.channelId != null) put("channelId", v.channelId)
            })
        }

        prefs.edit()
            .putString(KEY_FAVORITES, favArr.toString())
            .putString(KEY_PLAYLISTS, plObj.toString())
            .putString(KEY_VIDEO_DETAILS, detailsObj.toString())
            .apply()
    }

    suspend fun syncFromDrive(accessToken: String): Triple<Set<String>, Map<String, List<String>>, Map<String, VideoItem>>? =
        withContext(Dispatchers.IO) {
            val fileId = resolveFileId(accessToken) ?: return@withContext null
            val req = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return@withContext null
            val body = res.body?.string().orEmpty()
            if (body.isBlank()) return@withContext null

            try {
                val json = JSONObject(body)
                val favs = mutableSetOf<String>()
                json.optJSONArray("favorites")?.let { arr ->
                    for (i in 0 until arr.length()) favs.add(arr.getString(i))
                }
                val pls = mutableMapOf<String, List<String>>()
                json.optJSONObject("playlists")?.let { obj ->
                    obj.keys().forEach { k ->
                        val arr = obj.getJSONArray(k)
                        pls[k] = (0 until arr.length()).map { arr.getString(it) }
                    }
                }
                val details = mutableMapOf<String, VideoItem>()
                json.optJSONObject("videoDetails")?.let { obj ->
                    obj.keys().forEach { k ->
                        val v = obj.getJSONObject(k)
                        details[k] = VideoItem(
                            id = v.optString("id", k),
                            source = if (v.optString("source") == "drive") VideoSource.DRIVE else VideoSource.YOUTUBE,
                            title = v.optString("title", ""),
                            category = v.optString("category", ""),
                            thumbnail = v.optString("thumbnail", ""),
                            duration = v.optString("duration").ifBlank { null },
                            durationSec = if (v.has("durationSec") && !v.isNull("durationSec")) v.optInt("durationSec") else null,
                            channel = v.optString("channel", ""),
                            channelId = v.optString("channelId").ifBlank { null }
                        )
                    }
                }
                saveLocal(favs, pls, details)
                Triple(favs, pls, details)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun pushToDrive(
        accessToken: String,
        favorites: Set<String>,
        playlists: Map<String, List<String>>,
        videoDetails: Map<String, VideoItem>
    ) = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("favorites", JSONArray(favorites.toList()))
            put("playlists", JSONObject().apply {
                playlists.forEach { (k, v) -> put(k, JSONArray(v)) }
            })
            put("videoDetails", JSONObject().apply {
                videoDetails.forEach { (id, v) ->
                    put(id, JSONObject().apply {
                        put("id", v.id)
                        put("source", if (v.source == VideoSource.DRIVE) "drive" else "youtube")
                        put("title", v.title)
                        put("category", v.category)
                        put("thumbnail", v.thumbnail)
                        if (v.duration != null) put("duration", v.duration)
                        if (v.durationSec != null) put("durationSec", v.durationSec)
                        put("channel", v.channel)
                        if (v.channelId != null) put("channelId", v.channelId)
                    })
                }
            })
        }.toString()

        val existingFileId = resolveFileId(accessToken)
        if (existingFileId != null) {
            val req = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media")
                .addHeader("Authorization", "Bearer $accessToken")
                .patch(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().close()
        } else {
            val folderId = prefs.getString(KEY_FOLDER_ID, "").orEmpty()
            val metadata = JSONObject().apply {
                put("name", "playlists.json")
                put("mimeType", "application/json")
                if (folderId.isNotBlank()) put("parents", JSONArray(listOf(folderId)))
            }.toString()

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(metadata.toRequestBody("application/json".toMediaType()))
                .addPart(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val req = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipart)
                .build()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val id = JSONObject(res.body?.string().orEmpty()).optString("id")
                if (id.isNotBlank()) prefs.edit().putString(KEY_FILE_ID, id).apply()
            }
            res.close()
        }
    }

    private fun resolveFileId(accessToken: String): String? {
        val cached = prefs.getString(KEY_FILE_ID, null)
        if (!cached.isNullOrBlank()) return cached

        val q = URLEncoder.encode("name = 'playlists.json' and trashed = false", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?q=$q&fields=files(id)"
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        val res = client.newCall(req).execute()
        if (!res.isSuccessful) return null
        val json = JSONObject(res.body?.string().orEmpty())
        val files = json.optJSONArray("files") ?: return null
        return if (files.length() > 0) {
            val id = files.getJSONObject(0).optString("id")
            prefs.edit().putString(KEY_FILE_ID, id).apply()
            id
        } else null
    }
}
