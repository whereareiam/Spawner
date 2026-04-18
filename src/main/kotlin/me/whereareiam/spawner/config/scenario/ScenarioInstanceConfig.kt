package me.whereareiam.spawner.config.scenario

import me.whereareiam.spawner.config.download.SpawnerDownloadConfig
import me.whereareiam.spawner.config.install.SpawnerFileInstallConfig
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class ScenarioInstanceConfig @Inject constructor(
	private val instanceName: String,
	private val objects: ObjectFactory
) {
	val rootOverlayDir: DirectoryProperty = objects.directoryProperty()
	private val installs = mutableListOf<SpawnerFileInstallConfig>()
	private val downloads = mutableListOf<SpawnerDownloadConfig>()

	fun getName(): String = instanceName

	fun install(action: Action<in SpawnerFileInstallConfig>) {
		val spec = objects.newInstance(SpawnerFileInstallConfig::class.java)
		action.execute(spec)
		installs += spec
	}

	fun installs(): List<SpawnerFileInstallConfig> = installs.toList()

	fun download(action: Action<in SpawnerDownloadConfig>) {
		val spec = objects.newInstance(SpawnerDownloadConfig::class.java)
		action.execute(spec)
		downloads += spec
	}

	fun modrinth(
		projectId: String,
		action: Action<in SpawnerDownloadConfig> = object : Action<SpawnerDownloadConfig> {
			override fun execute(t: SpawnerDownloadConfig) = Unit
		}
	) {
		download(
			object : Action<SpawnerDownloadConfig> {
				override fun execute(spec: SpawnerDownloadConfig) {
					spec.provider.set("modrinth")
					spec.identifier.set(projectId)
					action.execute(spec)
				}
			}
		)
	}

	fun downloads(): List<SpawnerDownloadConfig> = downloads.toList()
}
