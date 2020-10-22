package ru.den.podplay

import android.app.Application
import ru.den.podplay.worker.ClearTrashWorker
import timber.log.Timber

class PodcastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        registerWorkers()
    }

    private fun registerWorkers() {
        ClearTrashWorker.enqueueSelf(this)
    }
}
