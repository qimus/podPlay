package ru.den.podplay.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.den.podplay.model.Download
import ru.den.podplay.repository.PodcastRepo
import java.lang.IllegalArgumentException

class DownloadViewModelFactory(private val repo: PodcastRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadPodcastViewModel::class.java)) {
            return DownloadPodcastViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown viewModel class - ${modelClass.toString()}")
    }
}

class DownloadPodcastViewModel(val repo: PodcastRepo) : ViewModel() {
    fun getAll(): LiveData<List<Download>> {
        return repo.findAllDownloads()
    }

    fun delete(download: Download) {
        repo.delete(download)
    }

    fun save(download: Download) {
        repo.save(download)
    }
}