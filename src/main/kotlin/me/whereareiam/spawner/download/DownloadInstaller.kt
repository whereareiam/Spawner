package me.whereareiam.spawner.download

import me.whereareiam.spawner.file.installFile
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.model.download.DownloadPlan
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

internal fun installDownloads(
	runtime: RuntimeSettings,
	downloads: List<DownloadPlan>,
	baseDir: File,
	cacheNamespace: String,
	logger: Logger
) {
	if (downloads.isEmpty()) return
	val cacheDir = runtime.downloadCacheDir.resolve(cacheNamespace)
	cacheDir.mkdirs()

	for (download in downloads) {
		val resolved = resolveDownload(
			downloadProviders = runtime.downloadProviders,
			defaultProvider = runtime.defaultDownloadProvider,
			userAgent = runtime.userAgent,
			download = download
		)
		val resolvedFileName = download.fileName?.takeIf { it.isNotBlank() }
			?: resolved.fileName
			?: throw GradleException("Download '${download.identifier}' did not resolve a file name.")
		val cachedFile = cacheDir.resolve(resolvedFileName)

		if (!cachedFile.exists() || runtime.forceDownload) {
			logger.lifecycle("Downloading ${download.identifier} (${resolved.version}) from ${resolved.url}")
			cachedFile.parentFile.mkdirs()
			downloadFile(resolved.url, cachedFile, runtime.userAgent)
		}

		installFile(baseDir, download.into, cachedFile, resolvedFileName)
	}
}
