package me.whereareiam.spawner.download.provider

import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.model.download.ResolvedDownload

interface DownloadProvider {
	fun resolve(download: DownloadPlan, userAgent: String): ResolvedDownload
}