package ru.den.podplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.den.podplay.R
import ru.den.podplay.db.PodplayDatabase
import ru.den.podplay.model.Download
import ru.den.podplay.model.Episode
import ru.den.podplay.repository.PodcastRepo
import ru.den.podplay.util.FileUtils
import ru.den.podplay.util.ProgressDownloadCallback
import ru.den.podplay.model.DownloadStatus

class DownloadService : Service() {
    private val downloadList = mutableListOf<String>()
    private var isStarted = false
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var podcastRepo: PodcastRepo

    companion object {

        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL = "DOWNLOAD_CHANNEL_ID"
        const val ACTION_DOWNLOAD_PODCAST = "run.den.podplay.DOWNLOAD_PODCAST"

        fun startDownload(context: Context, guid: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra("guid", guid)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(ACTION_DOWNLOAD_PODCAST)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val db = PodplayDatabase.getInstance(this)
        podcastRepo = PodcastRepo(FeedService.instance, db.podcastDao())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val guid = intent?.getStringExtra("guid") ?: return START_NOT_STICKY

        if (guid != "") {
            downloadList.add(guid)
            if (!isStarted) {
                isStarted = true
                createNotification()
                startDownloadQueue()
            } else {
                updateNotification()
            }
        } else if (!isStarted) {
            stopForeground(true)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startDownloadQueue() {
        GlobalScope.launch(Dispatchers.IO) {
            while (downloadList.size > 0) {
                val guid = downloadList.removeAt(0)
                download(guid)
            }
            isStarted = false
            stopForeground(true)
            stopSelf()
        }
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
        notificationBuilder
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle("Start downloading")

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL, "DownloadManager", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(download: Download? = null) {
        download?.let {
            notificationBuilder.setContentText(download.episodeTitle)
        }

        if (downloadList.size > 0) {
            notificationBuilder.setContentTitle(
                getString(R.string.download_queue, downloadList.size))
        } else {
            notificationBuilder.setContentTitle("Download podcast")
        }

        showNotification(notificationBuilder.build())
    }

    private fun showNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun download(guid: String) {
        val download = podcastRepo.findDownload(guid) ?: return
        updateNotification(download)

        val file = FileUtils.getFileFor(this, download)
        download.file = file.absolutePath
        file.setReadable(true, false)

        var totalBytesCount = 0
        FileUtils.download(file, download.mediaUrl!!, object : ProgressDownloadCallback {
            override fun onStart(bytesCount: Int) {
                totalBytesCount = bytesCount
                download.size = bytesCount.toLong()
                podcastRepo.save(download)
                notificationBuilder.setProgress(bytesCount, 0, false)
                showNotification(notificationBuilder.build())
            }

            override fun onProgress(progress: Int) {
                notificationBuilder.setProgress(totalBytesCount, progress, false)
                showNotification(notificationBuilder.build())
            }

            override fun onComplete() {
                notificationBuilder.setProgress(0, 0, false)
                download.status = DownloadStatus.DOWNLOADED
                podcastRepo.save(download)
                showNotification(notificationBuilder.build())

                val intent = Intent().apply {
                    action = ACTION_DOWNLOAD_PODCAST
                    putExtra("guid", guid)
                }
                sendBroadcast(intent)
            }
        })
    }
}
