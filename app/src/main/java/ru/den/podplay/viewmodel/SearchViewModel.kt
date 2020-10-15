package ru.den.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.den.podplay.repository.ItunesRepo
import ru.den.podplay.service.ItunesPodcast
import ru.den.podplay.util.DateUtils

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    var itunesRepo: ItunesRepo? = null

    private var _podcasts: MutableLiveData<List<PodcastSummaryViewData>> = MutableLiveData(listOf())
    val podcasts: LiveData<List<PodcastSummaryViewData>>
        get() = _podcasts

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchTerm: MutableLiveData<String> = MutableLiveData("")

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
            itunesPodcast.artworkUrl100,
            itunesPodcast.feedUrl
        )
    }

    fun searchPodcasts(term: String, limit: Int = 30, offset: Int = 0) {
        isLoading.value = true
        searchTerm.value = term
        itunesRepo?.searchByTerm(term, limit, offset) { results ->
            isLoading.value = false
            if (results == null) {
                _podcasts.postValue(listOf())
            } else {
                val searchViews = results.map { podcast -> itunesPodcastToPodcastSummary(podcast) }
                _podcasts.postValue(searchViews)
            }
        }
    }
}
