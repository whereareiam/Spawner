package me.whereareiam.spawner

import me.whereareiam.spawner.target.SpawnerTarget
import me.whereareiam.spawner.target.proxy.velocity.VelocityProxyTarget
import me.whereareiam.spawner.target.server.paper.PaperServerTarget
import org.gradle.api.Project

fun registerSpawnerTasks(project: Project, config: SpawnerConfig) {
	val targets: List<SpawnerTarget> = listOf(
		PaperServerTarget(),
		VelocityProxyTarget()
	)
	val registered = targets.map { target -> target to target.registerTasks(project, config) }

	val prepareDevServers = project.tasks.register("prepareDevServers") {
		group = "devserver"
		description = "Prepare dev servers for the current platform."
		dependsOn(registered.map { it.second.prepare })
	}

	project.tasks.register("runDevServers") {
		group = "devserver"
		description = "Run dev servers for the current platform."
		dependsOn(prepareDevServers)
		doLast {
			for ((target, _) in registered) {
				if (target.isEnabled(config)) {
					target.startDetached(project, config)
				}
			}
		}
	}
}

