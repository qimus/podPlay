package ru.den.podplay

import android.app.Application
import timber.log.Timber

class PodcastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}