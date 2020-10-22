package ru.den.podplay.worker

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.coroutineScope
import ru.den.podplay.util.FileUtils
import java.util.concurrent.TimeUnit

class ClearTrashWorker(applicationContext: Context, params: WorkerParameters)
    : CoroutineWorker(applicationContext, params) {

    companion object {
        private const val WORK_TAG = "ClearTrashWorker"
        private const val WORK_NAME = "ru.den.podplay.worker.ClearTrashWorker"

        fun enqueueSelf(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val clearTrashRequest = PeriodicWorkRequestBuilder<ClearTrashWorker>(4, TimeUnit.HOURS)
                .addTag(WORK_TAG)
                .build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, clearTrashRequest)
        }
    }

    override suspend fun doWork() = coroutineScope {
        val trashDir = FileUtils.getTrashDir(applicationContext)
        trashDir.list()?.forEach { fileName ->
            applicationContext.deleteFile(fileName)
        }
        Result.success()
    }
}
