package ru.den.podplay.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_episode_player.*
import ru.den.podplay.R
import ru.den.podplay.service.PodplayMediaCallback
import ru.den.podplay.service.PodplayMediaService
import ru.den.podplay.util.HtmlUtils
import timber.log.Timber

class EpisodePlayerFragment : Fragment() {
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var playerSpeed: Float = 1f
    private var episodeDuration = 0L
    private var draggingScrubber = false
    private var progressAnimator: ValueAnimator? = null
    private var mediaSession: MediaSessionCompat? = null
    private var exoPlayer: ExoPlayer? = null
    private var playOnPrepare = false
    private var isVideo = false
    private var playAfterResume = false
    private var isPlaying = false

    private var mediaInfo: MediaInfo? = null

    companion object {
        fun newInstance(mediaInfo: MediaInfo): EpisodePlayerFragment {
            return EpisodePlayerFragment().apply {
                val bundle = Bundle().apply {
                    putParcelable("mediaInfo", mediaInfo)
                }
                arguments = bundle
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        mediaInfo = arguments?.getParcelable("mediaInfo") as? MediaInfo

        isVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaInfo?.isVideo ?: false
        } else {
            false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_episode_player, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (isVideo) {
            initMediaSession()
            initMediaPlayer()
        }
        setupControls()
        updateControls()
    }

    override fun onStart() {
        super.onStart()

        if (!isVideo) {
            initMediaBrowser()
        } else {
            initMediaSession()
            initMediaPlayer()
        }

        (activity as PodcastActivity).hideBottomNavBar()

        if (!isVideo) {
            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else {
                mediaBrowser.connect()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity).unregisterCallback(it)
            }
        }

        if (!fragmentActivity.isChangingConfigurations) {
            if (isPlaying) {
                exoPlayer?.pause()
                playAfterResume = true
            }
        }

        if (!isVideo) {
            mediaBrowser.disconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDetach() {
        super.onDetach()
        (activity as PodcastActivity).showBottomNavBar()
    }

    private fun initMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(activity as Context, "EpisodePlayerFragment")
            mediaSession?.setMediaButtonReceiver(null)
        }
        registerMediaController(mediaSession!!.sessionToken)
    }

    private fun initMediaPlayer() {
        if (exoPlayer == null) {
            exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
            exoPlayerView.player = exoPlayer
            val fragmentActivity = activity as FragmentActivity
            val episodeMediaCallback = PodplayMediaCallback(fragmentActivity,
                mediaSession!!, exoPlayer)
            mediaSession!!.setCallback(episodeMediaCallback)
        } else {
            exoPlayerView.player = exoPlayer
        }

        if (playOnPrepare && playAfterResume) {
            togglePlayPause()
            playAfterResume = false
        }
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun setupVideoUi() {
        episodeDescTextView.visibility = View.INVISIBLE
        headerView.visibility = View.INVISIBLE
        val activity = activity as? AppCompatActivity
        activity?.supportActionBar?.hide()
        playerControls.setBackgroundColor(Color.argb(255/2, 0, 0, 0))
    }

    private fun updateControlsFromController() {
        val controller = MediaControllerCompat.getMediaController(requireActivity())
        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChange(controller.playbackState)
                updateControlsFromMetadata(metadata)
            }
        }
    }

    private fun togglePlayPause() {
        playOnPrepare = true
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                startPlaying()
            }
        } else {
            startPlaying()
        }
    }

    private fun updateControls() {
        episodeTitleTextView.text = mediaInfo?.title
        val htmlDesc = mediaInfo?.description ?: ""
        HtmlUtils.setTextViewHTML(episodeDescTextView, htmlDesc) { span ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(span.url)
            }
            startActivity(intent)
        }

        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity)
            .load(mediaInfo?.imageUrl)
            .into(episodeImageView)

        exoPlayer?.let {
            updateControlsFromController()
        }
    }

    private fun changeSpeed() {
        playerSpeed += 0.25f
        if (playerSpeed > 2f) {
            playerSpeed = 0.75f
        }

        val bundle = Bundle().apply {
            putFloat(PodplayMediaCallback.CMD_EXTRA_SPEED, playerSpeed)
        }
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(PodplayMediaCallback.CMD_CHANGE_SPEED, bundle, null)
        speedButton.text = "${playerSpeed}x"
    }

    private fun startPlaying() {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        val mediaInfo = mediaInfo ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaInfo.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaInfo.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, mediaInfo.imageUrl)
        Timber.d("imageUrl: ${mediaInfo.imageUrl}")
        if (mediaInfo.file != null) {
            controller.transportControls.playFromUri(Uri.parse(mediaInfo.file), bundle)
        } else {
            controller.transportControls.playFromUri(Uri.parse(mediaInfo.mediaUrl), bundle)
        }
    }

    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity, ComponentName(
            fragmentActivity, PodplayMediaService::class.java
        ), MediaBrowserCallbacks(), null)
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position + seconds * 1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        endTimeTextView.text = DateUtils.formatElapsedTime(episodeDuration / 1000)
        seekBar.max = episodeDuration.toInt()
    }

    private fun setupControls() {
        activity?.title = mediaInfo?.title ?: ""
        playToggleButton.setOnClickListener { togglePlayPause() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            speedButton.setOnClickListener { changeSpeed() }
        } else {
            speedButton.visibility = View.INVISIBLE
        }

        forwardButton.setOnClickListener { seekBy(30) }
        replayButton.setOnClickListener { seekBy(-10) }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentTimeTextView.text = DateUtils.formatElapsedTime((progress / 1000).toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                draggingScrubber = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                draggingScrubber = false
                val fragmentActivity = activity as FragmentActivity
                val controller = MediaControllerCompat.getMediaController(fragmentActivity)
                if (controller.playbackState != null) {
                    controller.transportControls.seekTo(seekBar.progress.toLong())
                } else {
                    seekBar.progress = 0
                }
            }
        })
    }

    private fun animateScrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()
        if (timeRemaining < 0) {
            return
        }

        progressAnimator = ValueAnimator.ofInt(progress, episodeDuration.toInt())
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
    }

    private fun handleStateChange(state: PlaybackStateCompat) {
        val playbackState = state.state
        val extras = state.extras
        isPlaying = playbackState == PlaybackStateCompat.STATE_PLAYING

        Timber.d("handleStateChange $state")

        if (playbackState == PlaybackStateCompat.STATE_ERROR) {
            val message = extras?.getString("message") ?: ""
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

        playToggleButton.isActivated = isPlaying
        val progress = state.position.toInt()
        seekBar.progress = progress
        speedButton.text = "${playerSpeed}x"

        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }

        if (state.state == PlaybackStateCompat.STATE_BUFFERING) {
            replayButton.isEnabled = false
            forwardButton.isEnabled = false
            playToggleButton.visibility = View.INVISIBLE
            progress_bar.visibility = View.VISIBLE
        } else {
            replayButton.isEnabled = true
            forwardButton.isEnabled = true
            playToggleButton.visibility = View.VISIBLE
            progress_bar.visibility = View.GONE
        }

        if (isPlaying) {
            if (isVideo) {
                setupVideoUi()
            }
            animateScrubber(progress, state.playbackSpeed)
        }
    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            @Suppress("NAME_SHADOWING") val state = state ?: return
            handleStateChange(state)
        }
    }

    inner class MediaBrowserCallbacks : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            updateControlsFromController()
            Timber.d("onConnected")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Timber.d("onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Timber.d("onConnectionFailed")
        }
    }

    @Parcelize
    data class MediaInfo(
        val feedTitle: String? = "",
        val title: String? = "",
        val description: String? = "",
        val isVideo: Boolean = false,
        val mediaUrl: String? = "",
        val imageUrl: String? = "",
        val file: String? = ""
    ) : Parcelable
}
