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
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.net.URI

class PodplayMediaCallback(
    val context: Context,
    val mediaSession: MediaSessionCompat,
    var mediaPlayer: MediaPlayer? = null
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
        println("onPlay called")
    }

    override fun onStop() {
        super.onStop()
        println("onStop called")
        stopPlaying()
        listener?.onStopPlaying()
    }

    override fun onPause() {
        super.onPause()
        println("onPause called")
        pausePlaying()
        listener?.onPausePlaying()
    }

    private fun setState(state: Int, newSpeed: Float? = null) {
        var position: Long = -1
        mediaPlayer?.let {
            position = it.currentPosition.toLong()
        }

        var speed = 1f
        if (newSpeed != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val state = mediaSession.controller.playbackState.state
                if (newSpeed == null) {
                    speed = mediaPlayer?.playbackParams?.speed ?: 1f
                } else {
                    speed = newSpeed
                }
                mediaPlayer?.let { mediaPlayer ->
                    try {
                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
                    } catch (e: Exception) {
                        mediaPlayer.reset()
                        mediaUri?.let { mediaUri -> mediaPlayer.setDataSource(context, mediaUri) }
                    }
                    mediaPlayer.prepare()
                    mediaPlayer.playbackParams.setSpeed(speed)
                    mediaPlayer.seekTo(position.toInt())
                    if (state == PlaybackStateCompat.STATE_PLAYING) {
                        mediaPlayer.start()
                    }
                }
            }
        }

        val playbackState = PlaybackStateCompat.Builder()
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

        mediaPlayer?.seekTo(pos.toInt())
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
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setOnCompletionListener {
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
            mediaNeedsPrepare = true
        }
    }

    private fun prepareMediaPlayer() {
        if (newMedia) {
            newMedia = false
            mediaPlayer?.run {
                try {
                    if (mediaNeedsPrepare) {
                        reset()
                        setDataSource(context, mediaUri!!)
                        prepare()
                    }
                    mediaExtras?.let { mediaExtras ->
                        mediaSession.setMetadata(MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                mediaPlayer?.duration?.toLong() ?: 0)
                            .build())
                    }
                } catch (e: Exception) {
                    setState(PlaybackStateCompat.STATE_ERROR)
                }

            }
        }
    }

    private fun startPlaying() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun pausePlaying() {
        removeAudioFocus()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun stopPlaying() {
        removeAudioFocus()
        mediaSession.isActive = false
        mediaPlayer?.let {
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
}
