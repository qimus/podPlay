package ru.den.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ru.den.podplay.repository.ItunesRepo
import ru.den.podplay.service.ItunesPodcast
import ru.den.podplay.util.DateUtils

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    var itunesRepo: ItunesRepo? = null

    data class PodcastSummaryViewData(
        var name: String? = "",
        var lastUpdated: String? = "",
        val imageUrl: String? = "",
        var feedUri: String? = ""
    )

    private fun itunesPodcastToPodcastSummary(
        itunesPodcast: ItunesPodcast): PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            DateUtils.jsonDateToShortDate(itunesPodcast.releaseDate),
            itunesPodcast.artworkUrl30,
            itunesPodcast.feedUrl
        )
    }

    fun searchPodcasts(term: String, callback: (List<PodcastSummaryViewData>) -> Unit) {
        itunesRepo?.searchByTerm(term) { results ->
            if (results == null) {
                callback(emptyList())
            } else {
                val searchViews = results.map { podcast -> itunesPodcastToPodcastSummary(podcast) }
                callback(searchViews)
            }
        }
    }
}
