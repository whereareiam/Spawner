package me.whereareiam.spawner.platform.target.server.paper.config

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class PaperStandaloneConfig @Inject constructor(objects: ObjectFactory) {
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
