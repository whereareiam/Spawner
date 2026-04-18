package me.whereareiam.spawner.config.install

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class SpawnerFileInstallConfig @Inject constructor(objects: ObjectFactory) {
	val files: ConfigurableFileCollection = objects.fileCollection()
	val into: Property<String> = objects.property(String::class.java).convention("")

	fun from(vararg items: Any) {
		files.from(*items)
	}

	fun into(path: String) {
		into.set(path)
	}
}
