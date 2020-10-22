package ru.den.podplay.model

import androidx.room.*
import java.io.File
import java.util.*

enum class DownloadStatus(val value: Int) {
    NOT_DOWNLOADED(0),
    DOWNLOADING(1),
    DOWNLOADED(2);

    companion object {
        fun valueOf(value: Int): DownloadStatus {
            for (item in values()) {
                if (item.value == value) {
                    return item
                }
            }

            throw IllegalAccessException()
        }
    }
}

class DownloadStatusConverter {
    @TypeConverter
    fun fromStatus(status: DownloadStatus?): Int? {
        return status?.value
    }

    @TypeConverter
    fun toStatus(status: Int?): DownloadStatus? {
        return DownloadStatus.valueOf(status ?: 0)
    }
}

@Entity(
    indices = [Index(value = ["guid"], unique = true)]
)
@TypeConverters(DownloadStatusConverter::class)
data class Download(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    var guid: String = "",
    @ColumnInfo(name = "podcast_title")
    var podcastTitle: String? = "",
    @ColumnInfo(name = "podcast_desc")
    var podcastDesc: String? = "",
    @ColumnInfo(name = "episode_title")
    var episodeTitle: String? = "",
    @ColumnInfo(name = "episode_desc")
    var episodeDesc: String? = "",
    var type: String? = "",
    @ColumnInfo(name = "release_date")
    var releaseDate: Date? = Date(),
    @ColumnInfo(name = "media_url")
    var mediaUrl: String? = "",
    @ColumnInfo(name = "image_url")
    var imageUrl: String? = "",
    var duration: String? = "",
    var status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    var file: String = "",
    var size: Long = 0
) {
    fun getFileIfExists(): File? {
        if (this.file.isEmpty()) return null
        val file = File(this.file)
        return if (file.exists()) file else null
    }
}
