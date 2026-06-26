package com.example.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log
import com.example.data.MediaStateHolder

class VoidMediaListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updatePlaybackState(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata(metadata)
        }
    }

    private val positionUpdater = object : Runnable {
        override fun run() {
            activeController?.playbackState?.let { state ->
                if (state.state == PlaybackState.STATE_PLAYING) {
                    val lastUpdate = state.lastPositionUpdateTime
                    val currentPos = state.position
                    val speed = state.playbackSpeed
                    if (lastUpdate > 0) {
                        val delta = System.currentTimeMillis() - lastUpdate
                        val interpolated = currentPos + (delta * speed).toLong()
                        val duration = MediaStateHolder.trackDuration.value
                        MediaStateHolder.trackPosition.value = if (duration > 0) {
                            Math.min(interpolated, duration)
                        } else {
                            interpolated
                        }
                    }
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        setupCallbacks()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val componentName = ComponentName(this, VoidMediaListenerService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(activeSessionsListener, componentName)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveController(controllers)
            handler.post(positionUpdater)
        } catch (e: SecurityException) {
            Log.e("VoidMediaListener", "Notification listener access not yet granted: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        handler.removeCallbacks(positionUpdater)
        super.onListenerDisconnected()
    }

    private fun setupCallbacks() {
        MediaStateHolder.playPauseCallback = {
            activeController?.let { controller ->
                val state = controller.playbackState?.state
                if (state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
        }
        MediaStateHolder.skipNextCallback = {
            activeController?.transportControls?.skipToNext()
        }
        MediaStateHolder.skipPreviousCallback = {
            activeController?.transportControls?.skipToPrevious()
        }
        MediaStateHolder.seekToCallback = { pos ->
            activeController?.transportControls?.seekTo(pos)
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        // Unregister from old controller
        activeController?.unregisterCallback(controllerCallback)

        // Find the active playing controller, or fallback to the first one
        val newController = controllers?.firstOrNull { 
            it.playbackState?.state == PlaybackState.STATE_PLAYING 
        } ?: controllers?.firstOrNull()

        activeController = newController

        if (newController != null) {
            newController.registerCallback(controllerCallback)
            updateMetadata(newController.metadata)
            updatePlaybackState(newController.playbackState)
            MediaStateHolder.activePackageName.value = newController.packageName
        } else {
            MediaStateHolder.trackTitle.value = "No active track"
            MediaStateHolder.trackArtist.value = "Press Play in Spotify/VLC"
            MediaStateHolder.trackArtwork.value = null
            MediaStateHolder.isPlaying.value = false
            MediaStateHolder.trackDuration.value = 0L
            MediaStateHolder.trackPosition.value = 0L
            MediaStateHolder.activePackageName.value = null
        }
    }

    private fun updatePlaybackState(state: PlaybackState?) {
        if (state != null) {
            MediaStateHolder.isPlaying.value = state.state == PlaybackState.STATE_PLAYING
            MediaStateHolder.trackPosition.value = state.position
        } else {
            MediaStateHolder.isPlaying.value = false
            MediaStateHolder.trackPosition.value = 0L
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata != null) {
            MediaStateHolder.trackTitle.value = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
            MediaStateHolder.trackArtist.value = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            MediaStateHolder.trackDuration.value = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
            
            // Try getting the large artwork first, fallback to standard or description icon
            val artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata.description.iconBitmap
            MediaStateHolder.trackArtwork.value = artwork
        } else {
            MediaStateHolder.trackTitle.value = "Unknown Track"
            MediaStateHolder.trackArtist.value = "Unknown Artist"
            MediaStateHolder.trackDuration.value = 0L
            MediaStateHolder.trackArtwork.value = null
        }
    }

    override fun onDestroy() {
        activeController?.unregisterCallback(controllerCallback)
        mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        handler.removeCallbacks(positionUpdater)
        super.onDestroy()
    }
}
