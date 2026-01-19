package me.whereareiam.spawner

import me.whereareiam.spawner.download.DownloadProvider
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
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

	val paper: PaperServerConfig = objects.newInstance(PaperServerConfig::class.java)
	val velocity: VelocityProxyConfig = objects.newInstance(VelocityProxyConfig::class.java)
	internal val downloadProviders: MutableMap<String, DownloadProvider> = mutableMapOf()

	fun registerDownloadProvider(name: String, provider: DownloadProvider) {
		downloadProviders[name.lowercase()] = provider
	}
}

abstract class PaperServerConfig @Inject constructor(objects: ObjectFactory) {
	val extraFiles: ConfigurableFileCollection = objects.fileCollection()
	val extraFilesDir: DirectoryProperty = objects.directoryProperty()
	val downloadProvider: Property<String> = objects.property(String::class.java)
		.convention("")
	val version: Property<String> = objects.property(String::class.java)
	val port: Property<Int> = objects.property(Int::class.java)
		.convention(25566)
	val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)
		.convention(listOf("-Xms1G", "-Xmx1G"))
	val onlineMode: Property<Boolean> = objects.property(Boolean::class.java)
		.convention(false)
	val acceptEula: Property<Boolean> = objects.property(Boolean::class.java)
		.convention(true)
	val pluginJar: RegularFileProperty = objects.fileProperty()
}

abstract class VelocityProxyConfig @Inject constructor(objects: ObjectFactory) {
	val extraFiles: ConfigurableFileCollection = objects.fileCollection()
	val extraFilesDir: DirectoryProperty = objects.directoryProperty()
	val downloadProvider: Property<String> = objects.property(String::class.java)
		.convention("")
	val version: Property<String> = objects.property(String::class.java)
	val port: Property<Int> = objects.property(Int::class.java)
		.convention(25565)
	val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)
		.convention(listOf("-Xms1G", "-Xmx1G"))
	val onlineMode: Property<Boolean> = objects.property(Boolean::class.java)
		.convention(false)
	val forwardingMode: Property<String> = objects.property(String::class.java)
		.convention("none")
	val pluginJar: RegularFileProperty = objects.fileProperty()
}
