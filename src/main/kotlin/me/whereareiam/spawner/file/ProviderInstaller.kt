package me.whereareiam.spawner.file

import me.whereareiam.spawner.SpawnerConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal fun installProviderJars(pluginDataDir: File, config: SpawnerConfig) {
	val jars = config.providerJars.files
	if (jars.isEmpty()) return
	val providersDir = File(pluginDataDir, "providers")
	providersDir.mkdirs()
	for (jar in jars) {
		Files.copy(
			jar.toPath(),
			providersDir.resolve(jar.name).toPath(),
			StandardCopyOption.REPLACE_EXISTING
		)
	}
}




