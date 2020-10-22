package ru.den.podplay.util

import android.content.Context
import android.webkit.MimeTypeMap
import ru.den.podplay.model.Download
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

interface ProgressDownloadCallback {
    fun onStart(bytesCount: Int)
    fun onProgress(progress: Int)
    fun onComplete()
}

object FileUtils {
    fun getTrashDir(context: Context): File {
        return File(context.filesDir, "trash")
    }

    fun getFileFor(context: Context, download: Download): File {
        val dir = File(context.filesDir, "podcasts")
        if (!dir.exists()) {
            dir.mkdirs()
            dir.setReadable(true, false)
        }

        val fileName = "${download.id}.${MimeTypeMap.getFileExtensionFromUrl(download.mediaUrl)}"
        return File(dir, fileName)
    }

    fun getFileName(path: String): String {
        return File(path).name
    }

    fun download(file: File, url: String, callback: ProgressDownloadCallback?) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.doInput = true

        connection.connect()

        val inputStream = connection.inputStream
        val outputStream = FileOutputStream(file)
        callback?.onStart(connection.contentLength)
        outputStream.use { output ->
            val buffer = ByteArray(4 * 1024)
            var byteCount = inputStream.read(buffer)
            var totalRead = byteCount

            while (byteCount > 0) {
                callback?.onProgress(totalRead)
                output.write(buffer)
                byteCount = inputStream.read(buffer)
                totalRead += byteCount
            }
            callback?.onComplete()

            output.flush()
        }

        connection.disconnect()
    }

    fun moveFile(source: File, dest: File) {
        val input = FileInputStream(source)
        val out = if (dest.isDirectory) {
            val outputFile = File(dest, source.name)
            FileOutputStream(outputFile)
        } else {
            FileOutputStream(dest)
        }

        input.use {
            out.use {
                val buffer = ByteArray(1024)
                var readCount = input.read(buffer)

                while (readCount > 0) {
                    out.write(buffer)
                    readCount = input.read(buffer)
                }
            }
        }

        source.delete()
    }
}
