package me.whereareiam.spawner.target

import me.whereareiam.spawner.SpawnerConfig
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

internal interface SpawnerTarget {
	val id: String
	fun isEnabled(config: SpawnerConfig): Boolean
	fun registerTasks(project: Project, config: SpawnerConfig): TargetTasks
	fun startDetached(project: Project, config: SpawnerConfig)
}

internal data class TargetTasks(
	val prepare: TaskProvider<out Task>,
	val run: TaskProvider<out Task>
)

