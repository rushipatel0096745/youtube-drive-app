package com.youtubevault.app.data

import android.content.Context
import com.youtubevault.app.model.WatchProgress
import org.json.JSONObject

class WatchHistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("yt_watch_history", Context.MODE_PRIVATE)

    fun saveProgress(videoId: String, currentTimeSec: Long, durationSec: Long) {
        if (durationSec <= 0) return
        val json = JSONObject().apply {
            put("currentTime", currentTimeSec)
            put("duration", durationSec)
            put("updatedAt", System.currentTimeMillis())
        }
        prefs.edit().putString(videoId, json.toString()).apply()
    }

    fun getProgress(videoId: String): WatchProgress? {
        val raw = prefs.getString(videoId, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            WatchProgress(
                currentTime = obj.optLong("currentTime", 0L),
                duration = obj.optLong("duration", 0L),
                updatedAt = obj.optLong("updatedAt", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }
}
