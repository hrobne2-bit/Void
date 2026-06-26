package com.example.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

object MediaStateHolder {
    val trackTitle = MutableStateFlow("No active track")
    val trackArtist = MutableStateFlow("Press Play in Spotify/VLC")
    val trackArtwork = MutableStateFlow<Bitmap?>(null)
    val isPlaying = MutableStateFlow(false)
    val trackDuration = MutableStateFlow(0L) // in milliseconds
    val trackPosition = MutableStateFlow(0L) // in milliseconds
    val activePackageName = MutableStateFlow<String?>(null)

    // User playback action callbacks
    var playPauseCallback: (() -> Unit)? = null
    var skipNextCallback: (() -> Unit)? = null
    var skipPreviousCallback: (() -> Unit)? = null
    var seekToCallback: ((Long) -> Unit)? = null

    fun triggerPlayPause() {
        playPauseCallback?.invoke()
    }

    fun triggerSkipNext() {
        skipNextCallback?.invoke()
    }

    fun triggerSkipPrevious() {
        skipPreviousCallback?.invoke()
    }

    fun triggerSeekTo(positionMs: Long) {
        seekToCallback?.invoke(positionMs)
    }
}

// Utility helper to parse color hex strings
fun parseColorHex(hex: String, fallback: Color): Color {
    return try {
        if (hex.startsWith("#")) {
            Color(android.graphics.Color.parseColor(hex))
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

// Format time from milliseconds (e.g. 182000 -> "3:02")
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

