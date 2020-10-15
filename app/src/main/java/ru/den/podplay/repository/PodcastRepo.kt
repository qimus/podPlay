package ru.den.podplay.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.den.podplay.db.PodcastDao
import ru.den.podplay.model.*
import ru.den.podplay.service.FeedService
import ru.den.podplay.service.RssFeedResponse
import ru.den.podplay.util.DateUtils

class PodcastRepo(
    private val feedService: FeedService,
    private val podcastDao: PodcastDao
) {
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {
        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)

            if (podcast != null) {
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    addDownloadInfoToEpisodes(podcast)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {
                feedService.getFeed(feedUrl) { feedResponse ->
                    var podcast: Podcast? = null
                    if (feedResponse != null) {
                        podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
                    }
                    addDownloadInfoToEpisodes(podcast)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            }
        }
    }

    private fun addDownloadInfoToEpisodes(podcast: Podcast?) {
        if (podcast == null) return
        val guids = podcast.episodes.map { it.guid }
        val downloads = podcastDao.findDownloadsByGuid(guids).map { it.guid to it }.toMap()
        podcast.episodes.forEach { episode ->
            if (downloads.containsKey(episode.guid)) {
                episode.download = downloads[episode.guid]
            }
        }
    }

    private fun rssItemsToEpisodes(episodeResponses: List<RssFeedResponse.EpisodeResponse>): List<Episode> {
        return episodeResponses.map {
            Episode(
                null,
                it.guid ?: "",
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }

    private fun rssResponseToPodcast(feedUrl: String, imageUrl: String, rssResponse: RssFeedResponse): Podcast? {
        val items = rssResponse.episodes ?: return null
        val description = if (rssResponse.description == "")
            rssResponse.summary else rssResponse.description

        return Podcast(null, feedUrl, rssResponse.title, description, imageUrl,
            rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items))
    }

    private fun getNewEpisodes(localPodcast: Podcast, callback: (List<Episode>) -> Unit) {
        feedService.getFeed(localPodcast.feedUrl) { response ->
            if (response != null) {
                val remotePodcast = rssResponseToPodcast(localPodcast.feedUrl,
                    localPodcast.imageUrl, response)
                val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)
                val newEpisodes = remotePodcast?.episodes?.filter { episode ->
                    localEpisodes.find { episode.guid == it.guid } == null
                }
                callback(newEpisodes ?: listOf())
            } else {
                callback(listOf())
            }
        }
    }

    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit) {
        val updatedPodcasts = mutableListOf<PodcastUpdateInfo>()
        val podcasts = podcastDao.loadPodcastsStatic()
        var processCount = podcasts.count()

        for (podcast in podcasts) {
            getNewEpisodes(podcast) { newEpisodes ->
                if (newEpisodes.count() > 0) {
                    saveNewEpisodes(podcast.id!!, newEpisodes)
                    updatedPodcasts.add(
                        PodcastUpdateInfo(podcast.feedUrl, podcast.feedTitle, newEpisodes.count())
                    )
                    processCount--
                    if (processCount == 0) {
                        callback(updatedPodcasts)
                    }
                }
            }
        }
    }

    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    fun save(podcast: Podcast) {
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)
            for (episode in podcast.episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun findEpisodeById(id: Long): Episode? {
        val episode = podcastDao.findEpisodeById(id) ?: return null
        val download = podcastDao.findDownloadByGuid(episode.guid)
        episode.download = download

        return episode
    }

    fun findDownload(guid: String): Download? {
        return podcastDao.findDownloadByGuid(guid)
    }

    fun findAllDownloads(): LiveData<List<Download>> {
        return podcastDao.loadDownloads()
    }

    fun save(episode: Episode) {
        GlobalScope.launch {
            podcastDao.insertEpisode(episode)
        }
    }

    fun save(download: Download) {
        GlobalScope.launch {
            podcastDao.insertDownloadInfo(download)
        }
    }

    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    fun delete(download: Download) {
        GlobalScope.launch {
            podcastDao.deleteDownload(download)
        }
    }

    class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)
}
