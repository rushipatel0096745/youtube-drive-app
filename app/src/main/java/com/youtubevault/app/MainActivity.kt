package com.youtubevault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youtubevault.app.model.AppThemeMode
import com.youtubevault.app.ui.components.ChannelSheet
import com.youtubevault.app.ui.components.PlaylistModal
import com.youtubevault.app.ui.screens.HomeScreen
import com.youtubevault.app.ui.screens.PlayerScreen
import com.youtubevault.app.ui.screens.SignInScreen
import com.youtubevault.app.ui.theme.YouTubeVaultTheme
import com.youtubevault.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()

            val accessToken by vm.accessToken.collectAsState()
            val userEmail by vm.userEmail.collectAsState()
            val authError by vm.authError.collectAsState()
            val recoverableIntent by vm.recoverableAuthIntent.collectAsState()
            val themeMode by vm.themeMode.collectAsState()
            val privateMode by vm.privateMode.collectAsState()
            val activeVideo by vm.activeVideo.collectAsState()
            val videos by vm.displayedVideos.collectAsState()
            val isLoading by vm.isLoading.collectAsState()
            val ytQuotaExceeded by vm.ytQuotaExceeded.collectAsState()
            val sourceFilter by vm.sourceFilter.collectAsState()
            val selectedCategory by vm.selectedCategory.collectAsState()
            val searchQuery by vm.searchQuery.collectAsState()
            val sortOrder by vm.sortOrder.collectAsState()
            val favorites by vm.favorites.collectAsState()
            val playlists by vm.playlists.collectAsState()
            val selectedPlaylistName by vm.selectedPlaylistName.collectAsState()
            val playlistModalVideo by vm.playlistModalVideo.collectAsState()
            val channelModalInfo by vm.channelModalInfo.collectAsState()
            val channelVideos by vm.channelVideos.collectAsState()
            val isChannelLoading by vm.isChannelLoading.collectAsState()

            val signInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                vm.onSignInResult(result.data)
            }

            val consentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                vm.clearRecoverableIntent()
                vm.trySilentRestore()
            }

            LaunchedEffect(recoverableIntent) {
                recoverableIntent?.let { intent ->
                    consentLauncher.launch(intent)
                }
            }

            YouTubeVaultTheme(
                darkTheme = themeMode == AppThemeMode.DARK,
                privateMode = privateMode
            ) {
                val token = accessToken
                if (token == null) {
                    SignInScreen(
                        errorMessage = authError,
                        onSignInClick = {
                            signInLauncher.launch(vm.authManager.getSignInIntent())
                        }
                    )
                } else {
                    val currentVideo = activeVideo
                    if (currentVideo != null) {
                        PlayerScreen(
                            video = currentVideo,
                            upNextVideos = videos,
                            accessToken = token,
                            resumeTimeSec = vm.getResumeTime(currentVideo.id),
                            isFavorite = favorites.contains(currentVideo.id),
                            onSelectVideo = vm::playVideo,
                            onToggleFavorite = { vm.toggleFavorite(currentVideo) },
                            onAddToPlaylist = { vm.openPlaylistModal(currentVideo) },
                            onChannelClick = if (currentVideo.channelId != null) {
                                { vm.openChannel(currentVideo) }
                            } else null,
                            onRandom = { vm.playRandomVideo() },
                            onProgress = { cur, dur -> vm.saveWatchProgress(currentVideo.id, cur, dur) },
                            onClose = { vm.closePlayer() }
                        )
                    } else {
                        HomeScreen(
                            userEmail = userEmail,
                            videos = videos,
                            isLoading = isLoading,
                            ytQuotaExceeded = ytQuotaExceeded,
                            sourceFilter = sourceFilter,
                            categories = vm.categories,
                            selectedCategory = selectedCategory,
                            searchQuery = searchQuery,
                            sortOrder = sortOrder,
                            privateMode = privateMode,
                            themeMode = themeMode,
                            favorites = favorites,
                            playlists = playlists,
                            selectedPlaylistName = selectedPlaylistName,
                            onSourceFilterChange = vm::setSourceFilter,
                            onCategorySelect = vm::selectCategory,
                            onSearchQueryChange = vm::setSearchQuery,
                            onSearchSubmit = vm::performYouTubeSearch,
                            onSortChange = vm::setSortOrder,
                            onTogglePrivateMode = vm::togglePrivateMode,
                            onToggleTheme = vm::toggleTheme,
                            onShuffle = vm::playRandomVideo,
                            onRefresh = vm::loadAllVideos,
                            onSignOut = vm::signOut,
                            onPlayVideo = vm::playVideo,
                            onToggleFavorite = vm::toggleFavorite,
                            onOpenPlaylistModal = vm::openPlaylistModal,
                            onSelectPlaylist = vm::selectPlaylist,
                            onDeletePlaylist = vm::deletePlaylist,
                            onOpenChannel = vm::openChannel
                        )
                    }

                    playlistModalVideo?.let { modalVideo ->
                        PlaylistModal(
                            video = modalVideo,
                            playlists = playlists,
                            onToggleVideo = vm::toggleVideoInPlaylist,
                            onCreatePlaylist = vm::createPlaylist,
                            onDismiss = vm::closePlaylistModal
                        )
                    }

                    channelModalInfo?.let { (_, channelName) ->
                        ChannelSheet(
                            channelName = channelName,
                            videos = channelVideos,
                            isLoading = isChannelLoading,
                            favorites = favorites,
                            onPlay = vm::playVideo,
                            onToggleFavorite = vm::toggleFavorite,
                            onAddToPlaylist = vm::openPlaylistModal,
                            onDismiss = vm::closeChannel
                        )
                    }
                }
            }
        }
    }
}
