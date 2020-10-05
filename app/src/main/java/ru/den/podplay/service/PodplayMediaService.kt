package ru.den.podplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaDescription
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.den.podplay.R
import ru.den.podplay.ui.PodcastActivity
import java.net.URI
import java.net.URL

class PodplayMediaService : MediaBrowserServiceCompat(), PodplayMediaCallback.PodplayMediaListener {
    private lateinit var mediaSession: MediaSessionCompat

    companion object {
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID = "PODPLAY_EMPTY_ROOT_MEDIA_ID"
        private const val PLAYER_CHANNEL_ID = "PLAYER_CHANNEL_ID"
        private const val NOTIFICATION_ID = 1
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "PodplayMediaService")
        sessionToken = mediaSession.sessionToken
        val callback = PodplayMediaCallback(this, mediaSession)
        callback.listener = this
        mediaSession.setCallback(callback)
    }

    private fun getPausePlayActions(): Pair<NotificationCompat.Action, NotificationCompat.Action> {
        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_pause_white, getString(R.string.pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                PlaybackStateCompat.ACTION_PAUSE)
        )

        val playAction = NotificationCompat.Action(
            R.drawable.ic_pause_white, getString(R.string.play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                PlaybackStateCompat.ACTION_PLAY)
        )

        return Pair(pauseAction, playAction)
    }

    private fun isPlaying(): Boolean {
        return if (mediaSession.controller.playbackState != null) {
            mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING
        } else {
            false
        }
    }

    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(this, PodcastActivity::class.java)
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(this,
            0, openActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(PLAYER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(PLAYER_CHANNEL_ID,
                "Player", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(mediaDescription: MediaDescriptionCompat, bitmap: Bitmap?): Notification {
        val notificationIntent = getNotificationIntent()
        val ( pauseAction, playAction ) = getPausePlayActions()
        val notification = NotificationCompat.Builder(this, PLAYER_CHANNEL_ID)

        notification
            .setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
            .setLargeIcon(bitmap)
            .setContentIntent(notificationIntent)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                    PlaybackStateCompat.ACTION_STOP)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_episode_icon)
            .addAction(if (isPlaying()) pauseAction else playAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_STOP)
                    )
            )

        return notification.build()
    }

    private fun displayNotification() {
        if (mediaSession.controller.metadata == null) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val mediaDescription = mediaSession.controller.metadata.description

        GlobalScope.launch(Dispatchers.IO) {
            val iconUrl = URL(mediaDescription.iconUri.toString())
            val bitmap = BitmapFactory.decodeStream(iconUrl.openStream())
            val notification = createNotification(mediaDescription, bitmap)

            ContextCompat.startForegroundService(this@PodplayMediaService,
                Intent(this@PodplayMediaService, PodplayMediaService::class.java)
            )

            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession.controller.transportControls.stop()
    }

    override fun onStateChanged() {
        displayNotification()
    }

    override fun onStopPlaying() {
        stopSelf()
        stopForeground(true)
    }

    override fun onPausePlaying() {
        stopForeground(false)
    }

    override fun onCreate() {
        super.onCreate()
        createMediaSession()
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == PODPLAY_EMPTY_ROOT_MEDIA_ID) {

        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot(
            PODPLAY_EMPTY_ROOT_MEDIA_ID, null
        )
    }
}
