package com.mrbastim.standby.extended.system

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn

data class MusicState(
    val accessGranted: Boolean = false,
    val hasSession: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false,
    val canToggle: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
)

/** Each visible music widget owns a subscription; stopping it releases all session callbacks. */
class MusicRepository(context: Context) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(this.context, MediaNotificationListener::class.java)
    private var selected: MediaController? = null

    private fun hasAccess() = context.getSystemService(NotificationManager::class.java)
        .isNotificationListenerAccessGranted(listener)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<MusicState> = MediaNotificationListener.changes.flatMapLatest {
        callbackFlow {
            val handler = Handler(Looper.getMainLooper())
            val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

            fun publish() {
                if (!hasAccess()) {
                    selected = null
                    trySend(MusicState())
                    return
                }
                val controllers = callbacks.keys
                val active = controllers.firstOrNull {
                    it.playbackState?.state == PlaybackState.STATE_PLAYING
                } ?: controllers.firstOrNull {
                    it.playbackState?.state == PlaybackState.STATE_BUFFERING
                } ?: controllers.firstOrNull {
                    it.sessionToken == selected?.sessionToken
                } ?: controllers.firstOrNull()
                selected = active
                if (active == null) {
                    trySend(MusicState(accessGranted = true))
                    return
                }
                val metadata = active.metadata
                val playback = active.playbackState
                val playing = playback?.state == PlaybackState.STATE_PLAYING ||
                    playback?.state == PlaybackState.STATE_BUFFERING
                val actions = playback?.actions ?: 0L
                fun supports(action: Long) = actions and action != 0L
                trySend(
                    MusicState(
                        accessGranted = true,
                        hasSession = true,
                        title = metadata?.description?.title?.toString().orEmpty(),
                        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                            ?: metadata?.description?.subtitle?.toString().orEmpty(),
                        artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                            ?: metadata?.description?.iconBitmap,
                        isPlaying = playing,
                        canToggle = supports(PlaybackState.ACTION_PLAY_PAUSE) ||
                            supports(if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY),
                        canSkipPrevious = supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
                        canSkipNext = supports(PlaybackState.ACTION_SKIP_TO_NEXT),
                    )
                )
            }

            fun attach(controllers: List<MediaController>) {
                callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
                callbacks.clear()
                controllers.forEach { controller ->
                    val callback = object : MediaController.Callback() {
                        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                        override fun onSessionDestroyed() {
                            callbacks.remove(controller)?.let { controller.unregisterCallback(it) }
                            publish()
                        }
                    }
                    callbacks[controller] = callback
                    controller.registerCallback(callback, handler)
                }
                publish()
            }

            val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener {
                attach(it.orEmpty())
            }
            var registered = false
            try {
                if (hasAccess()) {
                    manager.addOnActiveSessionsChangedListener(sessionsListener, listener, handler)
                    registered = true
                    attach(manager.getActiveSessions(listener))
                } else {
                    trySend(MusicState())
                }
            } catch (_: SecurityException) {
                selected = null
                trySend(MusicState())
            }
            awaitClose {
                if (registered) manager.removeOnActiveSessionsChangedListener(sessionsListener)
                callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
                callbacks.clear()
                selected = null
            }
        }
    }.flowOn(Dispatchers.Main.immediate)

    fun togglePlayback() = command { controller ->
        val playback = controller.playbackState
        val playing = playback?.state == PlaybackState.STATE_PLAYING ||
            playback?.state == PlaybackState.STATE_BUFFERING
        val action = if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY
        if ((playback?.actions ?: 0L) and (action or PlaybackState.ACTION_PLAY_PAUSE) != 0L) {
            if (playing) controller.transportControls.pause() else controller.transportControls.play()
        }
    }

    fun previous() = command {
        if ((it.playbackState?.actions ?: 0L) and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) {
            it.transportControls.skipToPrevious()
        }
    }

    fun next() = command {
        if ((it.playbackState?.actions ?: 0L) and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) {
            it.transportControls.skipToNext()
        }
    }

    private fun command(action: (MediaController) -> Unit) {
        try {
            if (hasAccess()) selected?.let(action)
        } catch (_: SecurityException) {
            selected = null
        }
    }
}
