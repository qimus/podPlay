package ru.den.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import ru.den.podplay.model.Download
import ru.den.podplay.model.Episode
import ru.den.podplay.model.Podcast
import ru.den.podplay.repository.PodcastRepo
import ru.den.podplay.util.DateUtils
import java.util.*

class PodcastViewModel(application: Application) :
    AndroidViewModel (application) {
    private var activePodcast: Podcast? = null
    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null
    var livePodcastData: LiveData<List<SearchViewModel.PodcastSummaryViewData>>? = null
    var activeEpisodeViewData: EpisodeViewData? = null

    data class PodcastViewData(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeViewData>
    )

    data class EpisodeViewData (
        var id: Long? = null,
        var guid: String = "",
        var podcastId: Long? = null,
        var title: String = "",
        var description: String = "",
        var mediaUrl: String = "",
        var type: String = "",
        var releaseDate: Date = Date(),
        var duration: String = "",
        var isVideo: Boolean = false,
        var downloadInfo: Download? = null
    )

    private fun episodesToEpisodesView(episodes: List<Episode>) =
        episodes.map {
            val isVideo = it.type.startsWith("video")
            EpisodeViewData(
                it.id, it.guid, it.podcastId, it.title, it.description, it.mediaUrl,
                it.type, it.releaseDate, it.duration, isVideo, it.download
            )
        }

    private fun podcastToPodcastView(podcast: Podcast) =
        PodcastViewData(
            podcast.id != null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )

    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData,
                   callback: (PodcastViewData?) -> Unit) {
        val repo = podcastRepo ?: return
        val feedUrl = podcastSummaryViewData.feedUri ?: return
        repo.getPodcast(feedUrl) {
            it?.let {
                it.feedTitle = podcastSummaryViewData.name ?: ""
                it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                activePodcastViewData = podcastToPodcastView(it)
                activePodcast = it
                callback(activePodcastViewData)
            }
        }
    }

    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>? {
        val repo = podcastRepo ?: return null

        if (livePodcastData == null) {
            val liveData = repo.getAll()
            livePodcastData = Transformations.map(liveData)
            { podcastList ->
                podcastList.map { podcast ->
                    podcastToSummaryView(podcast)
                }
            }
        }

        return livePodcastData
    }

    private fun podcastToSummaryView(podcast: Podcast): SearchViewModel.PodcastSummaryViewData =
        SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            DateUtils.dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl
        )

    fun saveActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.save(it)
        }
    }

    fun deleteActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.delete(it)
        }
    }

    fun setActivePodcast(feedUrl: String, callback: (SearchViewModel.PodcastSummaryViewData?) -> Unit) {
        val repo = podcastRepo ?: return
        repo.getPodcast(feedUrl) {
            if (it == null) {
                callback(null)
            } else {
                activePodcastViewData = podcastToPodcastView(it)
                activePodcast = it
                callback(podcastToSummaryView(it))
            }
        }
    }

    fun saveDownloadTask(download: Download) {
        val repo = podcastRepo ?: return
        repo.save(download)
    }
}
