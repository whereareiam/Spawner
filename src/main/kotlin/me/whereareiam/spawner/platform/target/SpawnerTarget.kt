package me.whereareiam.spawner.platform.target

import me.whereareiam.spawner.model.InstancePlan
import me.whereareiam.spawner.model.RuntimeSettings
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

internal interface SpawnerTarget<T : InstancePlan> {
	val id: String
	fun registerTasks(
		project: Project,
		runtime: RuntimeSettings,
		instance: T,
		taskSuffix: String
	): TargetTasks

	fun startDetached(project: Project, instance: T)
}

internal data class TargetTasks(
	val prepare: TaskProvider<out Task>,
	val run: TaskProvider<out Task>,
	val serverSpec: String
)
