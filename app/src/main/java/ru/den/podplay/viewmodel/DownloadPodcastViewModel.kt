package ru.den.podplay.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.den.podplay.model.Download
import ru.den.podplay.repository.PodcastRepo
import ru.den.podplay.util.FileUtils
import java.io.File
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

    fun delete(context: Context, download: Download) {
        repo.delete(download)
        download.getFileIfExists()?.let { file ->
            val trashDir = FileUtils.getTrashDir(context)
            if (!trashDir.exists()) {
                trashDir.mkdirs()
            }
            FileUtils.moveFile(file, trashDir)
        }
    }

    fun restore(context: Context, download: Download) {
        val trashDir = FileUtils.getTrashDir(context)
        val source = File(trashDir, FileUtils.getFileName(download.file))
        FileUtils.moveFile(source, File(download.file))
    }

    fun save(download: Download) {
        repo.save(download)
    }
}