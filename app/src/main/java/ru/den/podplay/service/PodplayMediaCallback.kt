package ru.den.podplay.service

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.*

class PodplayMediaCallback(
    val context: Context,
    val mediaSession: MediaSessionCompat,
    var exoPlayer: ExoPlayer? = null
) : MediaSessionCompat.Callback() {
    private var mediaUri: Uri? = null
    private var newMedia: Boolean = false
    private var mediaExtras: Bundle? = null
    private var mediaNeedsPrepare: Boolean = false

    private var focusRequest: AudioFocusRequest? = null

    var listener: PodplayMediaListener? = null

    companion object {
        const val CMD_CHANGE_SPEED = "change_speed"
        const val CMD_EXTRA_SPEED = "extra_speed"
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        println("Playing ${uri.toString()}, extras: $extras")

        if (mediaUri == uri) {
            newMedia = false
            mediaExtras = null
        } else {
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()
    }

    override fun onPlay() {
        super.onPlay()
        if (ensureAudioFocus()) {
            mediaSession.isActive = true
            initMediaPlayer()
            prepareMediaPlayer()
            startPlaying()
        }
    }

    override fun onStop() {
        super.onStop()
        stopPlaying()
        listener?.onStopPlaying()
    }

    override fun onPause() {
        super.onPause()
        pausePlaying()
        listener?.onPausePlaying()
    }

    private fun setState(state: Int, newSpeed: Float? = null, extras: Bundle? = null) {
        var position: Long = -1
        exoPlayer?.let {
            position = it.currentPosition / 1000
        }

        var speed = 1f
        if (newSpeed != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                val state = mediaSession.controller.playbackState.state
//                if (newSpeed == null) {
//                    speed = mediaPlayer?.playbackParams?.speed ?: 1f
//                } else {
//                    speed = newSpeed
//                }
//                mediaPlayer?.let { mediaPlayer ->
//                    try {
//                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
//                    } catch (e: Exception) {
//                        mediaPlayer.reset()
//                        mediaUri?.let { mediaUri -> mediaPlayer.setDataSource(context, mediaUri) }
//                    }
//                    mediaPlayer.prepare()
//                    mediaPlayer.playbackParams.setSpeed(speed)
//                    mediaPlayer.seekTo(position.toInt())
//                    if (state == PlaybackStateCompat.STATE_PLAYING) {
//                        mediaPlayer.start()
//                    }
//                }
            }
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setExtras(extras)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PAUSE)
            .setState(state, position, speed)
            .build()

        mediaSession.setPlaybackState(playbackState)

        if (state == PlaybackStateCompat.STATE_PAUSED ||
            state == PlaybackStateCompat.STATE_PLAYING
        ) {
            listener?.onStateChanged()
        }
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)

        exoPlayer?.seekTo(pos)
        val playbackState = mediaSession.controller.playbackState

        if (playbackState != null) {
            setState(playbackState.state)
        } else {
            setState(PlaybackStateCompat.STATE_PAUSED)
        }
    }

    private fun changeSpeed(extras: Bundle) {
        var playbackState = PlaybackStateCompat.STATE_PAUSED
        if (mediaSession.controller.playbackState != null) {
            playbackState = mediaSession.controller.playbackState.state
        }
        setState(playbackState, extras.getFloat(CMD_EXTRA_SPEED))
    }

    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        super.onCommand(command, extras, cb)
        when (command) {
            CMD_CHANGE_SPEED -> extras?.let { changeSpeed(it) }
        }
    }

    private fun setNewMedia(uri: Uri?) {
        newMedia = true
        mediaUri = uri
    }

    private fun initMediaPlayer() {
        if (exoPlayer == null) {
            exoPlayer = SimpleExoPlayer.Builder(context).build()
            exoPlayer!!.addListener(ExoPlayerEventListener())
            mediaNeedsPrepare = true
        } else {
            exoPlayer!!.addListener(ExoPlayerEventListener())
            mediaNeedsPrepare = true
        }
    }

    private fun prepareMediaPlayer() {
        if (newMedia) {
            newMedia = false
            exoPlayer?.run {
                setMediaItem(MediaItem.fromUri(mediaUri!!))
                setState(PlaybackStateCompat.STATE_BUFFERING)
                prepare()
            }
        }
    }

    private fun updateMetadata() {
        mediaExtras?.let { mediaExtras ->
            mediaSession.setMetadata(MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                    mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                .putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    exoPlayer?.duration ?: 0)
                .build())
        }
    }

    private fun startPlaying() {
        exoPlayer?.let {
            if (!it.isPlaying) {
                it.play()
            }
        }
    }

    private fun pausePlaying() {
        removeAudioFocus()
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun stopPlaying() {
        removeAudioFocus()
        mediaSession.isActive = false
        exoPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }
    }

    private fun ensureAudioFocus(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .run {
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setUsage(AudioAttributes.USAGE_MEDIA)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    })
                    build()
                }
            this.focusRequest = focusRequest
            val result = audioManager.requestAudioFocus(focusRequest)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            val result = audioManager.requestAudioFocus(null,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun removeAudioFocus() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    interface PodplayMediaListener {
        fun onStateChanged()
        fun onStopPlaying()
        fun onPausePlaying()
    }

    inner class ExoPlayerEventListener : Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException) {
            super.onPlayerError(error)
            val extras = Bundle().apply {
                putString("message", error.message)
            }
            setState(PlaybackStateCompat.STATE_ERROR, null, extras)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_BUFFERING) {
                setState(PlaybackStateCompat.STATE_BUFFERING)
            } else if (playbackState == Player.STATE_ENDED) {
                setState(PlaybackStateCompat.STATE_PAUSED)
            } else if (playbackState == Player.STATE_READY && !playWhenReady) {
                setState(PlaybackStateCompat.STATE_PAUSED)
            } else if (playbackState == Player.STATE_READY && playWhenReady) {
                updateMetadata()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }
}
