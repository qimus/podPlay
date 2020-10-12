package ru.den.podplay.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Podcast(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    @ColumnInfo(name = "feed_url")
    var feedUrl: String = "",
    @ColumnInfo(name = "feed_title")
    var feedTitle: String = "",
    @ColumnInfo(name = "feed_desc")
    var feedDesc: String = "",
    @ColumnInfo(name = "image_url")
    var imageUrl: String = "",
    @ColumnInfo(name = "last_updated")
    var lastUpdated: Date = Date(),
    @Ignore
    var episodes: List<Episode> = listOf()
)
