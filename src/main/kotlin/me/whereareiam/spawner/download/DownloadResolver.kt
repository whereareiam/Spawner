package me.whereareiam.spawner.download

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.download.provider.DownloadProvider
import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.model.download.ResolvedDownload
import org.gradle.api.GradleException

internal fun resolveDownload(
	config: SpawnerConfig,
	download: DownloadPlan
): ResolvedDownload {
	return resolveDownload(
		downloadProviders = config.downloadProviders,
		defaultProvider = config.downloadProvider.orNull,
		userAgent = config.userAgent.get(),
		download = download
	)
}

internal fun resolveDownload(
    downloadProviders: Map<String, DownloadProvider>,
    defaultProvider: String?,
    userAgent: String,
    download: DownloadPlan
): ResolvedDownload {
	val name = download.provider?.takeIf { it.isNotBlank() }
		?: defaultProvider?.takeIf { it.isNotBlank() }
		?: "fill"
	val normalized = name.lowercase()
	val provider = downloadProviders[normalized]
		?: throw GradleException(
			"Unknown dev download provider '$name'. Available: ${downloadProviders.keys.sorted().joinToString(", ")}"
		)
	return provider.resolve(download.copy(provider = normalized), userAgent)
}
