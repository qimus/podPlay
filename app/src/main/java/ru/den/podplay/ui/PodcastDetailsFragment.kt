package ru.den.podplay.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_podcast_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.den.podplay.R
import ru.den.podplay.adapter.EpisodeListAdapter
import ru.den.podplay.model.Download
import ru.den.podplay.model.DownloadStatus
import ru.den.podplay.service.DownloadService
import ru.den.podplay.viewmodel.PodcastViewModel

class PodcastDetailsFragment : Fragment(), EpisodeListAdapter.EpisodeListAdapterListener {
    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener: OnPodcastDetailsListener? = null
    private var menuItem: MenuItem? = null
    private lateinit var downloadReceiver: DownloadBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        downloadReceiver = DownloadBroadcastReceiver()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnPodcastListener")
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(downloadReceiver)
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(downloadReceiver, DownloadService.getIntentFilter())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
        menuItem = menu.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_feed_action -> {
                podcastViewModel.activePodcastViewData?.feedUrl?.let {
                    val isSubscribed = podcastViewModel.activePodcastViewData?.subscribed ?: false
                    if (isSubscribed) {
                        listener?.onUnsubscribe()
                    } else {
                        listener?.onSubscribe()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl).into(feedImageView)
        }
    }

    private fun setupControls() {
        activity?.title = podcastViewModel.activePodcastViewData?.feedTitle ?: ""
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        episodeRecyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(episodeRecyclerView.context,
            layoutManager.orientation)
        episodeRecyclerView.addItemDecoration(dividerItemDecoration)

        episodeListAdapter = EpisodeListAdapter(
            podcastViewModel.activePodcastViewData?.episodes,
            this
        )
        episodeRecyclerView.adapter = episodeListAdapter
    }

    private fun updateMenuItem() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        menuItem?.title = if (viewData.subscribed) getString(R.string.unsubscribe)
            else getString(R.string.subscribe)
    }

    override fun onSelectEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        listener?.onShowEpisodePlayer(episodeViewData)
    }

    override fun onDownloadEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        if (episodeViewData.mediaUrl.isEmpty()) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val download = Download(
                null,
                episodeViewData.guid,
                podcastViewModel.activePodcastViewData?.feedTitle,
                podcastViewModel.activePodcastViewData?.feedDesc,
                episodeViewData.title,
                episodeViewData.description,
                episodeViewData.type,
                episodeViewData.releaseDate,
                episodeViewData.mediaUrl,
                podcastViewModel.activePodcastViewData?.imageUrl,
                episodeViewData.duration,
                DownloadStatus.DOWNLOADING
            )
            podcastViewModel.saveDownloadTask(download)
            episodeViewData.downloadInfo = download
            DownloadService.startDownload(activity as Context, episodeViewData.guid)
            withContext(Dispatchers.Main) {
                episodeListAdapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    inner class DownloadBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val guid = intent?.getStringExtra("guid") ?: return
            val repo = podcastViewModel.podcastRepo ?: return
            GlobalScope.launch {
                val download = repo.findDownload(guid)
                var isNeedUpdateList = false
                podcastViewModel.activePodcastViewData?.episodes?.forEach { episode ->
                    if (episode.guid == guid) {
                        episode.downloadInfo = download
                        isNeedUpdateList = true
                    }
                }
                if (isNeedUpdateList) {
                    withContext(Dispatchers.Main) {
                        episodeListAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }
}
