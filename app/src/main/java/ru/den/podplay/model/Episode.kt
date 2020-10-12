package ru.den.podplay.model

import androidx.room.*
import java.util.*

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcast_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcast_id"), Index("id")]
)
data class Episode(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var guid: String = "",
    var title: String = "",
    var description: String = "",
    @ColumnInfo(name = "media_url")
    var mediaUrl: String = "",
    var type: String = "",
    @ColumnInfo(name = "release_date")
    var releaseDate: Date = Date(),
    var duration: String = "",
    @ColumnInfo(name = "podcast_id")
    var podcastId: Long? = null,
    @Ignore
    var download: Download? = null
)
