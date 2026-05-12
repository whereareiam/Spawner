package me.whereareiam.spawner

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.download.provider.FillApiDownloadProvider
import me.whereareiam.spawner.download.provider.ModrinthDownloadProvider
import me.whereareiam.spawner.download.provider.SpigotJenkinsDownloadProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

class SpawnerPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		val config = project.extensions.create("spawner", SpawnerConfig::class.java)
		config.registerDownloadProvider("fill", FillApiDownloadProvider())
		config.registerDownloadProvider("modrinth", ModrinthDownloadProvider())
		config.registerDownloadProvider("spigot-jenkins", SpigotJenkinsDownloadProvider())
		applyPropertyConventions(project, config)
		validateTypes(config)

		project.plugins.withId("com.github.johnrengelman.shadow") {
			val shadowJar = project.tasks.named("shadowJar", Jar::class.java)
			config.velocity.pluginJar.convention(shadowJar.flatMap { it.archiveFile })
		}

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

		config.paper.port.convention(
			project.providers.gradleProperty("dev.paper.port").map { it.toInt() }.orElse(25566)
		)
		config.paper.version.convention(project.providers.gradleProperty("dev.paper.version"))
		config.paper.downloadProvider.convention(
			project.providers.gradleProperty("dev.paper.download.provider").orElse("")
		)
		config.paper.jvmArgs.convention(
			project.providers.gradleProperty("dev.paper.jvmArgs")
				.map { it.split(" ").filter(String::isNotBlank) }
				.orElse(listOf("-Xms1G", "-Xmx1G"))
		)
			config.paper.onlineMode.convention(
				project.providers.gradleProperty("dev.paper.onlineMode").map { it.toBoolean() }.orElse(false)
			)

			config.bungeecord.port.convention(
				project.providers.gradleProperty("dev.bungeecord.port").map { it.toInt() }.orElse(25565)
			)
			config.bungeecord.version.convention(project.providers.gradleProperty("dev.bungeecord.version"))
			config.bungeecord.downloadProvider.convention(
				project.providers.gradleProperty("dev.bungeecord.download.provider").orElse("spigot-jenkins")
			)
			config.bungeecord.jvmArgs.convention(
				project.providers.gradleProperty("dev.bungeecord.jvmArgs")
					.map { it.split(" ").filter(String::isNotBlank) }
					.orElse(listOf("-Xms1G", "-Xmx1G"))
			)
			config.bungeecord.onlineMode.convention(
				project.providers.gradleProperty("dev.bungeecord.onlineMode").map { it.toBoolean() }.orElse(false)
			)
			config.bungeecord.ipForward.convention(
				project.providers.gradleProperty("dev.bungeecord.ipForward").map { it.toBoolean() }.orElse(true)
			)

			config.velocity.port.convention(
				project.providers.gradleProperty("dev.velocity.port").map { it.toInt() }.orElse(25565)
			)
		config.velocity.version.convention(project.providers.gradleProperty("dev.velocity.version"))
		config.velocity.downloadProvider.convention(
			project.providers.gradleProperty("dev.velocity.download.provider").orElse("")
		)
		config.velocity.jvmArgs.convention(
			project.providers.gradleProperty("dev.velocity.jvmArgs")
				.map { it.split(" ").filter(String::isNotBlank) }
				.orElse(listOf("-Xms1G", "-Xmx1G"))
		)
		config.velocity.onlineMode.convention(
			project.providers.gradleProperty("dev.velocity.onlineMode").map { it.toBoolean() }.orElse(false)
		)
		config.velocity.forwardingMode.convention(
			project.providers.gradleProperty("dev.velocity.forwardingMode").orElse("none")
		)
	}
}
