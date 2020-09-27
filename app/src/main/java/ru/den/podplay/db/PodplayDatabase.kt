package ru.den.podplay.db

import android.content.Context
import androidx.room.*
import ru.den.podplay.model.Episode
import ru.den.podplay.model.Podcast
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? = date?.time
}

@Database(entities = [Podcast::class, Episode::class], version = 1)
@TypeConverters(Converters::class)
abstract class PodplayDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    companion object {
        private var instance: PodplayDatabase? = null

        fun getInstance(context: Context): PodplayDatabase {
            instance = Room.databaseBuilder(context.applicationContext,
                PodplayDatabase::class.java, "PodPlayer").build()

            return instance as PodplayDatabase
        }
    }
}
