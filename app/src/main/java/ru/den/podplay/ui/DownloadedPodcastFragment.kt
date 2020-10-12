package ru.den.podplay.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_downloaded_podcast.*
import ru.den.podplay.R
import ru.den.podplay.adapter.DownloadPodcastAdapter
import ru.den.podplay.db.PodplayDatabase
import ru.den.podplay.model.Download
import ru.den.podplay.repository.PodcastRepo
import ru.den.podplay.service.FeedService
import ru.den.podplay.viewmodel.DownloadPodcastViewModel
import ru.den.podplay.viewmodel.DownloadViewModelFactory
import timber.log.Timber

class DownloadedPodcastFragment : Fragment() {
    private lateinit var downloadViewModel: DownloadPodcastViewModel
    private lateinit var downloadPodcastAdapter: DownloadPodcastAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val db = PodplayDatabase.getInstance(requireContext())
        val repo = PodcastRepo(FeedService.instance, db.podcastDao())
        val viewModelFactory = DownloadViewModelFactory(repo)
        downloadViewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(DownloadPodcastViewModel::class.java)

        return inflater.inflate(R.layout.fragment_downloaded_podcast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        downloadViewModel.getAll().observe(viewLifecycleOwner, Observer { downloads ->
            downloadPodcastAdapter.setItems(downloads.toMutableList())
        })
    }

    override fun onStart() {
        super.onStart()
        (activity as? BottomBarHolder)?.showBottomNavBar()
    }

    private fun setupAdapter() {
        downloadPodcastAdapter = DownloadPodcastAdapter { download ->
            showPlayerFragment(download)
        }
        rv_downloads.adapter = downloadPodcastAdapter
    }

    private fun showPlayerFragment(download: Download) {
        val mediaInfo = EpisodePlayerFragment.MediaInfo(
            download.podcastTitle, download.episodeTitle,
            download.episodeDesc, false, download.mediaUrl,
            download.imageUrl, download.file
        )

        fragmentManager?.beginTransaction()
            ?.replace(R.id.podcastDetailsContainer, EpisodePlayerFragment.newInstance(mediaInfo), PodcastActivity.TAG_PLAYER_FRAGMENT)
            ?.addToBackStack(PodcastActivity.TAG_PLAYER_FRAGMENT)
            ?.commit()
    }

    companion object {
        fun newInstance(): DownloadedPodcastFragment {
            return DownloadedPodcastFragment()
        }
    }
}
