package com.youtubevault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youtubevault.app.model.AppThemeMode
import com.youtubevault.app.model.SourceFilter
import com.youtubevault.app.ui.components.PlaylistModal
import com.youtubevault.app.ui.screens.ChannelScreen
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
            val activeChannel by vm.activeChannel.collectAsState()
            val channelMeta by vm.channelMeta.collectAsState()
            val channelTab by vm.channelTab.collectAsState()
            val channelVideos by vm.displayedChannelVideos.collectAsState()
            val isChannelLoading by vm.isChannelLoading.collectAsState()
            val videos by vm.displayedVideos.collectAsState()
            val isLoading by vm.isLoading.collectAsState()
            val ytQuotaExceeded by vm.ytQuotaExceeded.collectAsState()
            val sourceFilter by vm.sourceFilter.collectAsState()
            val selectedCategory by vm.selectedCategory.collectAsState()
            val searchQuery by vm.searchQuery.collectAsState()
            val sortOrder by vm.sortOrder.collectAsState()
            val favorites by vm.favorites.collectAsState()
            val playlists by vm.playlists.collectAsState()
            val playlistFolders by vm.playlistFolders.collectAsState()
            val openedPlaylistName by vm.openedPlaylistName.collectAsState()
            val playerQueueContext by vm.playerQueueContext.collectAsState()
            val playlistModalVideo by vm.playlistModalVideo.collectAsState()

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

            BackHandler(
                enabled = activeVideo != null ||
                    activeChannel != null ||
                    (sourceFilter == SourceFilter.PLAYLIST && openedPlaylistName != null)
            ) {
                vm.navigateBack()
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
                    val currentChannel = activeChannel

                    when {
                        currentVideo != null -> {
                            val queueCtx = playerQueueContext
                            PlayerScreen(
                                video = currentVideo,
                                upNextTitle = queueCtx?.let { "Up Next • ${it.first}" } ?: "Up Next",
                                upNextVideos = queueCtx?.second
                                    ?: if (currentChannel != null) channelVideos else videos,
                                accessToken = token,
                                resumeTimeSec = vm.getResumeTime(currentVideo.id),
                                isFavorite = favorites.contains(currentVideo.id),
                                onSelectVideo = { nextVid ->
                                    if (queueCtx != null) vm.playFromCurrentQueue(nextVid)
                                    else vm.playVideo(nextVid)
                                },
                                onToggleFavorite = { vm.toggleFavorite(currentVideo) },
                                onAddToPlaylist = { vm.openPlaylistModal(currentVideo) },
                                onChannelClick = { vm.openChannel(currentVideo) },
                                onRandom = { vm.playRandomVideo() },
                                onProgress = { cur, dur -> vm.saveWatchProgress(currentVideo.id, cur, dur) },
                                onClose = { vm.closePlayer() }
                            )
                        }
                        currentChannel != null -> {
                            ChannelScreen(
                                channelInfo = currentChannel,
                                channelMeta = channelMeta,
                                activeTab = channelTab,
                                videos = channelVideos,
                                isLoading = isChannelLoading,
                                favorites = favorites,
                                onTabChange = vm::setChannelTab,
                                onPlayVideo = vm::playVideo,
                                onToggleFavorite = vm::toggleFavorite,
                                onOpenPlaylistModal = vm::openPlaylistModal,
                                onBack = vm::closeChannel
                            )
                        }
                        else -> {
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
                                getProgressFraction = vm::getProgressFraction,
                                playlistFolders = playlistFolders,
                                openedPlaylistName = openedPlaylistName,
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
                                onPlayVideo = { vid -> vm.playVideo(vid) },
                                onPlayVideoFromPlaylist = { vid, plName, plVideos ->
                                    vm.playVideo(vid, plName, plVideos)
                                },
                                onToggleFavorite = vm::toggleFavorite,
                                onOpenPlaylistModal = vm::openPlaylistModal,
                                onOpenPlaylist = vm::openPlaylist,
                                onCloseOpenedPlaylist = vm::closeOpenedPlaylist,
                                onCreatePlaylist = { name -> vm.createPlaylist(name) },
                                onDeletePlaylist = vm::deletePlaylist,
                                onOpenChannel = vm::openChannel
                            )
                        }
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
                }
            }
        }
    }
}
