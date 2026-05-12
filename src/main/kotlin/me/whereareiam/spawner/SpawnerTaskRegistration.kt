package me.whereareiam.spawner

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.platform.*
import org.gradle.api.Project

fun registerSpawnerTasks(project: Project, config: SpawnerConfig) {
	project.afterEvaluate {
		val platforms = builtInPlatforms()
		validateConfiguredPlatforms(config, platforms)
		validateScenarioPlatforms(config, platforms)

		val runtime = RuntimeSettings(
			serverDir = config.serverDir.get().asFile,
			downloadProviders = config.downloadProviders.toMap(),
			defaultDownloadProvider = config.downloadProvider.get(),
			userAgent = config.userAgent.get(),
			forceDownload = config.forceDownload.get(),
			downloadCacheDir = project.layout.buildDirectory.dir("spawner-downloads").get().asFile
		)

		if (config.scenarios.isEmpty()) {
			registerStandalonePlatforms(project, config, runtime, platforms)
		} else {
			registerScenarioPlatforms(project, config, runtime, platforms)
		}
	}
}
