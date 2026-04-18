package me.whereareiam.spawner.config

import me.whereareiam.spawner.config.scenario.SpawnerScenariosConfig
import me.whereareiam.spawner.download.DownloadProvider
import me.whereareiam.spawner.target.proxy.velocity.config.VelocityStandaloneConfig
import me.whereareiam.spawner.target.server.paper.config.PaperStandaloneConfig
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class SpawnerConfig @Inject constructor(
	objects: ObjectFactory,
	layout: ProjectLayout
) {
	val serverDir: DirectoryProperty = objects.directoryProperty()
		.convention(layout.projectDirectory.dir("server"))
	val downloadProvider: Property<String> = objects.property(String::class.java)
		.convention("fill")
	val userAgent: Property<String> = objects.property(String::class.java)
		.convention("spawner/1.0")
	val forceDownload: Property<Boolean> = objects.property(Boolean::class.java)
		.convention(false)
	val serverType: Property<String> = objects.property(String::class.java)
		.convention("none")
	val proxyType: Property<String> = objects.property(String::class.java)
		.convention("none")
	val paper: PaperStandaloneConfig = objects.newInstance(PaperStandaloneConfig::class.java)
	val velocity: VelocityStandaloneConfig = objects.newInstance(VelocityStandaloneConfig::class.java)
	val scenarios: SpawnerScenariosConfig = objects.newInstance(SpawnerScenariosConfig::class.java)
	internal val downloadProviders: MutableMap<String, DownloadProvider> = mutableMapOf()

	fun registerDownloadProvider(name: String, provider: DownloadProvider) {
		downloadProviders[name.lowercase()] = provider
	}
}
