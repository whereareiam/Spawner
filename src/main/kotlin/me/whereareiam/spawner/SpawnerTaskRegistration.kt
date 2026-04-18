package me.whereareiam.spawner

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.model.RuntimeSettings
import org.gradle.api.Project

fun registerSpawnerTasks(project: Project, config: SpawnerConfig) {
	project.afterEvaluate {
		validateTypes(config)
		validateScenarios(config)

		val runtime = RuntimeSettings(
			serverDir = config.serverDir.get().asFile,
			downloadProviders = config.downloadProviders.toMap(),
			defaultDownloadProvider = config.downloadProvider.get(),
			userAgent = config.userAgent.get(),
			forceDownload = config.forceDownload.get(),
			downloadCacheDir = project.layout.buildDirectory.dir("spawner-downloads").get().asFile
		)

		if (config.scenarios.isEmpty()) {
			registerStandaloneMode(project, config, runtime)
		} else {
			registerScenarioMode(project, config, runtime)
		}
	}
}
