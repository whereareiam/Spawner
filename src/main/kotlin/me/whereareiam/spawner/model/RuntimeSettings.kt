package me.whereareiam.spawner.model

import me.whereareiam.spawner.download.provider.DownloadProvider
import java.io.File

data class RuntimeSettings(
    val serverDir: File,
    val downloadProviders: Map<String, DownloadProvider>,
    val defaultDownloadProvider: String,
    val userAgent: String,
    val forceDownload: Boolean,
    val downloadCacheDir: File
)