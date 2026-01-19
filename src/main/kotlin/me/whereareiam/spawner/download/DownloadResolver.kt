package me.whereareiam.spawner.download

import me.whereareiam.spawner.SpawnerConfig
import me.whereareiam.spawner.model.ResolvedDownload
import org.gradle.api.GradleException

internal fun resolveDownload(
	config: SpawnerConfig,
	project: String,
	version: String?,
	overrideProvider: String?
): ResolvedDownload {
	val name = overrideProvider?.takeIf { it.isNotBlank() }
		?: config.downloadProvider.orNull?.takeIf { it.isNotBlank() }
		?: "fill"
	val normalized = name.lowercase()
	val provider = config.downloadProviders[normalized]
		?: throw GradleException(
			"Unknown dev download provider '$name'. Available: ${config.downloadProviders.keys.sorted().joinToString(", ")}"
		)
	return provider.resolve(project, version, config.userAgent.get())
}

