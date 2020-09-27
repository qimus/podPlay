package ru.den.podplay.repository

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.den.podplay.service.ItunesPodcast
import ru.den.podplay.service.ItunesService
import ru.den.podplay.service.PodcastResponse

class ItunesRepo(private val itunesService: ItunesService) {
    fun searchByTerm(term: String, callback: (List<ItunesPodcast>?) -> Unit) {
        val podcastCall = itunesService.searchPodcastByTerm(term)

        podcastCall.enqueue(object : Callback<PodcastResponse> {
            override fun onFailure(call: Call<PodcastResponse>, t: Throwable) {
                callback(null)
            }

            override fun onResponse(
                call: Call<PodcastResponse>,
                response: Response<PodcastResponse>
            ) {
                val body = response.body()
                callback(body?.results)
            }
        })
    }
}
