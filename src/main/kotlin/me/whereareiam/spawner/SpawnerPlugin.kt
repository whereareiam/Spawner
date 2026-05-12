package me.whereareiam.spawner

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.download.provider.FillApiDownloadProvider
import me.whereareiam.spawner.download.provider.ModrinthDownloadProvider
import me.whereareiam.spawner.download.provider.SpigotJenkinsDownloadProvider
import me.whereareiam.spawner.platform.builtInPlatforms
import org.gradle.api.Plugin
import org.gradle.api.Project

class SpawnerPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		val config = project.extensions.create("spawner", SpawnerConfig::class.java)
		config.registerDownloadProvider("fill", FillApiDownloadProvider())
		config.registerDownloadProvider("modrinth", ModrinthDownloadProvider())
		config.registerDownloadProvider("spigot-jenkins", SpigotJenkinsDownloadProvider())
		applyPropertyConventions(project, config)
		for (platform in builtInPlatforms())
			platform.applyConventions(project, config)

		registerSpawnerTasks(project, config)
	}

	private fun applyPropertyConventions(project: Project, config: SpawnerConfig) {
		config.serverDir.convention(project.rootProject.layout.projectDirectory.dir("server"))
		config.downloadProvider.convention(
			project.providers.gradleProperty("dev.download.provider").orElse("fill")
		)
		config.userAgent.convention(
			project.providers.gradleProperty("dev.userAgent").orElse("spawner/1.0")
		)
		config.forceDownload.convention(
			project.providers.gradleProperty("dev.forceDownload").map { it.toBoolean() }.orElse(false)
		)
		config.serverType.convention(
			project.providers.gradleProperty("dev.server.type").orElse("none")
		)
		config.proxyType.convention(
			project.providers.gradleProperty("dev.proxy.type").orElse("none")
		)
	}
}
