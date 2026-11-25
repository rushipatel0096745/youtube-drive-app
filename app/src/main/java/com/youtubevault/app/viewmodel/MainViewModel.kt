package com.youtubevault.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youtubevault.app.auth.AuthResult
import com.youtubevault.app.auth.GoogleAuthManager
import com.youtubevault.app.data.DriveRepository
import com.youtubevault.app.data.PlaylistSyncManager
import com.youtubevault.app.data.WatchHistoryManager
import com.youtubevault.app.data.YouTubeRepository
import com.youtubevault.app.model.AppThemeMode
import com.youtubevault.app.model.SortOrder
import com.youtubevault.app.model.SourceFilter
import com.youtubevault.app.model.VideoItem
import com.youtubevault.app.model.VideoSource
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = GoogleAuthManager(application)
    private val driveRepo = DriveRepository()
    private val ytRepo = YouTubeRepository()
    private val playlistSync = PlaylistSyncManager(application)
    private val watchHistory = WatchHistoryManager(application)

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _recoverableAuthIntent = MutableStateFlow<Intent?>(null)
    val recoverableAuthIntent: StateFlow<Intent?> = _recoverableAuthIntent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ytQuotaExceeded = MutableStateFlow(false)
    val ytQuotaExceeded: StateFlow<Boolean> = _ytQuotaExceeded.asStateFlow()

    private var refreshCount = 0

    private val _driveVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _ytVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _searchResults = MutableStateFlow<List<VideoItem>?>(null)

    private val _sourceFilter = MutableStateFlow(SourceFilter.BLENDED)
    val sourceFilter: StateFlow<SourceFilter> = _sourceFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DEFAULT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _privateMode = MutableStateFlow(false)
    val privateMode: StateFlow<Boolean> = _privateMode.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _activeVideo = MutableStateFlow<VideoItem?>(null)
    val activeVideo: StateFlow<VideoItem?> = _activeVideo.asStateFlow()

    private val _favorites = MutableStateFlow(playlistSync.loadLocalFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow(playlistSync.loadLocalPlaylists())
    val playlists: StateFlow<Map<String, List<String>>> = _playlists.asStateFlow()

    private val _videoDetails = MutableStateFlow(playlistSync.loadLocalVideoDetails())

    private val _selectedPlaylistName = MutableStateFlow<String?>(null)
    val selectedPlaylistName: StateFlow<String?> = _selectedPlaylistName.asStateFlow()

    private val _playlistModalVideo = MutableStateFlow<VideoItem?>(null)
    val playlistModalVideo: StateFlow<VideoItem?> = _playlistModalVideo.asStateFlow()

    private val _channelModalInfo = MutableStateFlow<Pair<String, String>?>(null)
    val channelModalInfo: StateFlow<Pair<String, String>?> = _channelModalInfo.asStateFlow()

    private val _channelVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val channelVideos: StateFlow<List<VideoItem>> = _channelVideos.asStateFlow()

    private val _isChannelLoading = MutableStateFlow(false)
    val isChannelLoading: StateFlow<Boolean> = _isChannelLoading.asStateFlow()

    val categories: List<String>
        get() = if (_privateMode.value) YouTubeRepository.PRIVATE_CATEGORIES
        else YouTubeRepository.NORMAL_CATEGORIES

    val displayedVideos: StateFlow<List<VideoItem>> = combine(
        _driveVideos,
        _ytVideos,
        _searchResults,
        _sourceFilter,
        _selectedCategory,
        _sortOrder,
        _favorites,
        _playlists,
        _selectedPlaylistName,
        _videoDetails,
        _searchQuery
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val driveList = values[0] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val ytList = values[1] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val searchRes = values[2] as List<VideoItem>?
        val filter = values[3] as SourceFilter
        val category = values[4] as String
        val sort = values[5] as SortOrder
        @Suppress("UNCHECKED_CAST")
        val favs = values[6] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val pls = values[7] as Map<String, List<String>>
        val activePl = values[8] as String?
        @Suppress("UNCHECKED_CAST")
        val details = values[9] as Map<String, VideoItem>
        val query = (values[10] as String).trim().lowercase()

        val allCombined = (driveList + ytList).distinctBy { it.id }

        var list = when (filter) {
            SourceFilter.FAVOURITES -> {
                val merged = (allCombined + details.values).associateBy { it.id }
                favs.mapNotNull { merged[it] }
            }
            SourceFilter.PLAYLIST -> {
                val merged = (allCombined + details.values).associateBy { it.id }
                val targetName = activePl ?: pls.keys.firstOrNull()
                if (targetName != null) {
                    (pls[targetName] ?: emptyList()).mapNotNull { merged[it] }
                } else emptyList()
            }
            SourceFilter.DRIVE -> {
                val base = if (searchRes != null) searchRes.filter { it.source == VideoSource.DRIVE } else driveList
                base
            }
            SourceFilter.YOUTUBE -> {
                val base = searchRes ?: ytList
                base.filter { category == "All" || it.category == category || it.category == "Search" }
            }
            SourceFilter.BLENDED -> {
                if (searchRes != null) {
                    // Interleave matching Drive videos + YouTube search results
                    val dMatches = if (query.isNotEmpty()) {
                        driveList.filter { it.title.lowercase().contains(query) }
                    } else driveList
                    interleave(dMatches, searchRes)
                } else {
                    val ytFiltered = ytList.filter { category == "All" || it.category == category }
                    interleave(driveList, ytFiltered)
                }
            }
        }

        if (query.isNotEmpty() && searchRes == null) {
            list = list.filter {
                it.title.lowercase().contains(query) || it.channel.lowercase().contains(query)
            }
        }

        when (sort) {
            SortOrder.AZ -> list.sortedBy { it.title.lowercase() }
            SortOrder.DURATION -> list.sortedByDescending { it.durationSec ?: 0 }
            SortOrder.DEFAULT -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun interleave(first: List<VideoItem>, second: List<VideoItem>): List<VideoItem> {
        val result = ArrayList<VideoItem>(first.size + second.size)
        val max = maxOf(first.size, second.size)
        for (i in 0 until max) {
            if (i < first.size) result.add(first[i])
            if (i < second.size) result.add(second[i])
        }
        return result.distinctBy { it.id }
    }

    init {
        trySilentRestore()
    }

    fun trySilentRestore() {
        val account = authManager.getLastSignedInAccount() ?: return
        viewModelScope.launch {
            handleAuthResult(authManager.fetchAccessToken(account))
        }
    }

    fun onSignInResult(data: Intent?) {
        viewModelScope.launch {
            _authError.value = null
            handleAuthResult(authManager.handleSignInIntentResult(data))
        }
    }

    fun clearRecoverableIntent() {
        _recoverableAuthIntent.value = null
    }

    private suspend fun handleAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                _userEmail.value = result.email
                _accessToken.value = result.accessToken
                _authError.value = null
                loadAllVideos()
            }
            is AuthResult.RecoverableError -> {
                _recoverableAuthIntent.value = result.intent
            }
            is AuthResult.Error -> {
                _authError.value = result.message
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _accessToken.value = null
            _userEmail.value = ""
            _driveVideos.value = emptyList()
            _ytVideos.value = emptyList()
            _activeVideo.value = null
        }
    }

    fun loadAllVideos() {
        val token = _accessToken.value ?: return
        refreshCount++
        viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = null

            val driveDeferred = async { driveRepo.loadDriveVault(token) }
            val driveSyncDeferred = async { playlistSync.syncFromDrive(token) }
            val ytDeferred = async { loadYouTubeFeed(_selectedCategory.value, _privateMode.value) }

            val driveResult = driveDeferred.await()
            driveResult.folderId?.let { playlistSync.setFolderId(it) }

            driveSyncDeferred.await()?.let { (favs, pls, details) ->
                _favorites.value = favs
                _playlists.value = pls
                _videoDetails.value = details
            }

            val linkedYt = if (driveResult.linkedYouTubeIds.isNotEmpty()) {
                ytRepo.fetchVideosByIds(driveResult.linkedYouTubeIds, "Drive Links")
            } else emptyList()

            _driveVideos.value = (driveResult.driveVideos + linkedYt).distinctBy { it.id }
            _ytVideos.value = ytDeferred.await()
            _isLoading.value = false
        }
    }

    private suspend fun loadYouTubeFeed(category: String, isPrivate: Boolean): List<VideoItem> {
        return if (isPrivate) {
            val res = ytRepo.fetchPrivateMixedFallback(category, refreshCount)
            _ytQuotaExceeded.value = res.quotaExceeded
            res.videos
        } else {
            val res = ytRepo.fetchPopularVideos(category)
            _ytQuotaExceeded.value = res.quotaExceeded
            res.videos
        }
    }

    fun setSourceFilter(filter: SourceFilter) {
        _sourceFilter.value = filter
        if (filter == SourceFilter.PLAYLIST && _selectedPlaylistName.value == null) {
            _selectedPlaylistName.value = _playlists.value.keys.firstOrNull()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _searchResults.value = null
        viewModelScope.launch {
            _isLoading.value = true
            _ytVideos.value = loadYouTubeFeed(category, _privateMode.value)
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) _searchResults.value = null
    }

    fun performYouTubeSearch() {
        val q = _searchQuery.value.trim()
        if (q.isEmpty()) {
            _searchResults.value = null
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = ytRepo.searchVideos(q)
            if (res.quotaExceeded) {
                _ytQuotaExceeded.value = true
                // Fallback when 100-unit search quota is exhausted:
                // 1) In Private mode, pull 1-unit playlist mix & filter locally
                // 2) Filter already-loaded Drive + YouTube videos locally
                if (_privateMode.value) {
                    val fb = ytRepo.fetchPrivateMixedFallback(_selectedCategory.value, refreshCount)
                    val combined = (_ytVideos.value + fb.videos).distinctBy { it.id }
                    val localMatches = combined.filter {
                        it.title.contains(q, ignoreCase = true) || it.channel.contains(q, ignoreCase = true)
                    }
                    _searchResults.value = localMatches.ifEmpty { fb.videos }
                } else {
                    _searchResults.value = null // triggers local filter on displayedVideos
                }
            } else {
                _ytQuotaExceeded.value = false
                _searchResults.value = res.videos
            }
            _isLoading.value = false
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun togglePrivateMode() {
        _privateMode.value = !_privateMode.value
        _selectedCategory.value = "All"
        loadAllVideos()
    }

    fun toggleTheme() {
        _themeMode.value = if (_themeMode.value == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
    }

    fun playVideo(video: VideoItem) {
        _activeVideo.value = video
    }

    fun playRandomVideo() {
        val list = displayedVideos.value
        if (list.isNotEmpty()) {
            _activeVideo.value = list.random()
        }
    }

    fun closePlayer() {
        _activeVideo.value = null
    }

    fun getResumeTime(videoId: String): Long = watchHistory.getProgress(videoId)?.currentTime ?: 0L

    fun saveWatchProgress(videoId: String, currentSec: Long, durationSec: Long) {
        watchHistory.saveProgress(videoId, currentSec, durationSec)
    }

    fun toggleFavorite(video: VideoItem) {
        val updated = _favorites.value.toMutableSet()
        if (updated.contains(video.id)) updated.remove(video.id) else updated.add(video.id)
        _favorites.value = updated

        val details = _videoDetails.value.toMutableMap()
        details[video.id] = video
        _videoDetails.value = details

        persistAndSync()
    }

    fun openPlaylistModal(video: VideoItem) {
        _playlistModalVideo.value = video
    }

    fun closePlaylistModal() {
        _playlistModalVideo.value = null
    }

    fun selectPlaylist(name: String) {
        _selectedPlaylistName.value = name
    }

    fun createPlaylist(name: String, initialVideo: VideoItem? = null) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val map = _playlists.value.toMutableMap()
        val list = (map[clean] ?: emptyList()).toMutableList()
        if (initialVideo != null && !list.contains(initialVideo.id)) {
            list.add(initialVideo.id)
            val details = _videoDetails.value.toMutableMap()
            details[initialVideo.id] = initialVideo
            _videoDetails.value = details
        }
        map[clean] = list
        _playlists.value = map
        _selectedPlaylistName.value = clean
        persistAndSync()
    }

    fun toggleVideoInPlaylist(playlistName: String, video: VideoItem) {
        val map = _playlists.value.toMutableMap()
        val list = (map[playlistName] ?: emptyList()).toMutableList()
        if (list.contains(video.id)) list.remove(video.id) else list.add(video.id)
        map[playlistName] = list
        _playlists.value = map

        val details = _videoDetails.value.toMutableMap()
        details[video.id] = video
        _videoDetails.value = details

        persistAndSync()
    }

    fun deletePlaylist(name: String) {
        val map = _playlists.value.toMutableMap()
        map.remove(name)
        _playlists.value = map
        if (_selectedPlaylistName.value == name) {
            _selectedPlaylistName.value = map.keys.firstOrNull()
        }
        persistAndSync()
    }

    fun openChannel(video: VideoItem) {
        val chId = video.channelId ?: return
        _channelModalInfo.value = chId to video.channel
        _channelVideos.value = emptyList()
        viewModelScope.launch {
            _isChannelLoading.value = true
            _channelVideos.value = ytRepo.fetchChannelVideos(chId)
            _isChannelLoading.value = false
        }
    }

    fun closeChannel() {
        _channelModalInfo.value = null
    }

    private fun persistAndSync() {
        val favs = _favorites.value
        val pls = _playlists.value
        val details = _videoDetails.value
        playlistSync.saveLocal(favs, pls, details)
        val token = _accessToken.value ?: return
        viewModelScope.launch {
            playlistSync.pushToDrive(token, favs, pls, details)
        }
    }
}
