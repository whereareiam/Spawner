package me.whereareiam.spawner.config.download

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class SpawnerDownloadConfig @Inject constructor(objects: ObjectFactory) {
	val provider: Property<String> = objects.property(String::class.java).convention("modrinth")
	val identifier: Property<String> = objects.property(String::class.java)
	val version: Property<String> = objects.property(String::class.java)
	val versionType: Property<String> = objects.property(String::class.java).convention("release")
	val fileName: Property<String> = objects.property(String::class.java)
	val into: Property<String> = objects.property(String::class.java).convention("")

	fun into(path: String) {
		into.set(path)
	}
}
