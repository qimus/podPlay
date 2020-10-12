package ru.den.podplay.db

import androidx.lifecycle.LiveData
import androidx.room.*
import ru.den.podplay.model.Download
import ru.den.podplay.model.Episode
import ru.den.podplay.model.Podcast

@Dao
interface PodcastDao {
    @Query("SELECT * FROM Podcast ORDER BY feed_title")
    fun loadPodcasts(): LiveData<List<Podcast>>

    @Query("SELECT * FROM Podcast ORDER BY feed_title")
    fun loadPodcastsStatic(): List<Podcast>

    @Transaction
    @Query("SELECT * FROM Episode WHERE podcast_id = :podcastId ORDER BY release_date DESC")
    fun loadEpisodes(podcastId: Long): List<Episode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPodcast(podcast: Podcast): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEpisode(episode: Episode): Long

    @Query("SELECT * FROM Podcast WHERE feed_url = :url")
    fun loadPodcast(url: String): Podcast?

    @Query("SELECT * FROM Episode WHERE id = :id")
    fun findEpisodeById(id: Long): Episode?

    @Query("SELECT * FROM Download WHERE guid = :guid")
    fun findDownloadByGuid(guid: String): Download

    @Query("SELECT * FROM Download ORDER BY id")
    fun loadDownloads(): LiveData<List<Download>>

    @Query("SELECT * FROM Download WHERE guid IN (:guid)")
    fun findDownloadsByGuid(guid: List<String>): List<Download>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDownloadInfo(model: Download)

    @Update
    fun updateDownloadInfo(model: Download)

    @Delete
    fun deletePodcast(podcast: Podcast)
}
