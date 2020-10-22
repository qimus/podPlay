package ru.den.podplay.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_downloaded_podcast.*
import kotlinx.coroutines.launch
import ru.den.podplay.R
import ru.den.podplay.adapter.DownloadPodcastAdapter
import ru.den.podplay.adapter.SwipeToDeleteCallback
import ru.den.podplay.db.PodplayDatabase
import ru.den.podplay.model.Download
import ru.den.podplay.repository.PodcastRepo
import ru.den.podplay.service.FeedService
import ru.den.podplay.viewmodel.DownloadPodcastViewModel
import ru.den.podplay.viewmodel.DownloadViewModelFactory

class DownloadedPodcastFragment : Fragment(), DownloadPodcastAdapter.DownloadPodcastAdapterListener {
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
        activity?.title = "Downloads"
    }

    override fun onSelect(download: Download) {
        showPlayerFragment(download)
    }

    override fun onDeleted(download: Download) {
        lastDeletedRecord = download
        downloadViewModel.delete(requireContext(), download)
        showUndoSnackbar()
    }

    private fun showUndoSnackbar() {
        val snackbar = Snackbar.make(fragment_download_podcast_layout,
            getString(R.string.snackbar_undo_text), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.snackbar_undo_action)) { v ->
            lastDeletedRecord?.let {
                viewLifecycleOwner.lifecycleScope.launch {
                    downloadViewModel.save(it)
                    downloadViewModel.restore(requireContext(), it)
                    lastDeletedRecord = null
                    downloadPodcastAdapter.notifyDataSetChanged()
                }
            }
        }
        snackbar.show()
    }

    private fun setupAdapter() {
        downloadPodcastAdapter = DownloadPodcastAdapter(this)
        rv_downloads.adapter = downloadPodcastAdapter

        val layoutManager = LinearLayoutManager(requireContext())
        rv_downloads.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        rv_downloads.addItemDecoration(dividerItemDecoration)

        val callback = SwipeToDeleteCallback(downloadPodcastAdapter, requireContext())
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(rv_downloads)
    }

    private fun showPlayerFragment(download: Download) {
        val mediaInfo = EpisodePlayerFragment.MediaInfo(
            download.podcastTitle, download.episodeTitle,
            download.episodeDesc, false, download.mediaUrl,
            download.imageUrl, download.file
        )

        findNavController().navigate(
            DownloadedPodcastFragmentDirections.actionDownloadedPodcastFragmentToEpisodePlayerFragment(mediaInfo)
        )

    }

    companion object {
        fun newInstance(): DownloadedPodcastFragment {
            return DownloadedPodcastFragment()
        }
        private var lastDeletedRecord: Download? = null
    }
}
