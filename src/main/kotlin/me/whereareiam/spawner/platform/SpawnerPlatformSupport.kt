package me.whereareiam.spawner.platform

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.config.scenario.SpawnerScenarioConfig
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.platform.target.TargetTasks
import org.gradle.api.Project

internal interface SpawnerPlatformSupport {
	val id: String
	val role: TargetRole

	fun applyConventions(project: Project, config: SpawnerConfig) = Unit

	fun registerStandalone(
		project: Project,
		config: SpawnerConfig,
		runtime: RuntimeSettings
	): RegisteredPlatformTasks

	fun registerScenario(
		project: Project,
		scenario: SpawnerScenarioConfig,
		runtime: RuntimeSettings
	): List<RegisteredPlatformTasks>

	fun validateScenario(scenario: SpawnerScenarioConfig) = Unit
}

internal data class RegisteredPlatformTasks(
	val displayName: String,
	val prepareTaskName: String,
	val runTaskName: String,
	val targetTasks: TargetTasks
)
