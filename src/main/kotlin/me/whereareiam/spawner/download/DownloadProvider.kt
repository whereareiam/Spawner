package me.whereareiam.spawner.download

import me.whereareiam.spawner.model.ResolvedDownload

interface DownloadProvider {
	fun resolve(project: String, version: String?, userAgent: String): ResolvedDownload
}

