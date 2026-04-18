package me.whereareiam.spawner

import me.whereareiam.spawner.config.SpawnerConfig
import org.gradle.api.GradleException

fun isPaperServer(config: SpawnerConfig): Boolean {
	return config.serverType.get().equals("paper", true)
}

fun isVelocityProxy(config: SpawnerConfig): Boolean {
	return config.proxyType.get().equals("velocity", true)
}

fun validateTypes(config: SpawnerConfig) {
	val serverType = config.serverType.orNull?.lowercase() ?: "none"
	val proxyType = config.proxyType.orNull?.lowercase() ?: "none"
	val serverSupported = setOf("none", "paper")
	val proxySupported = setOf("none", "velocity")
	if (serverType !in serverSupported) {
		throw GradleException("Unsupported dev.server.type '$serverType'. Supported: ${serverSupported.joinToString(", ")}")
	}
	if (proxyType !in proxySupported) {
		throw GradleException("Unsupported dev.proxy.type '$proxyType'. Supported: ${proxySupported.joinToString(", ")}")
	}
}

fun validateScenarios(config: SpawnerConfig) {
	for (scenario in config.scenarios.all()) {
		for (instance in scenario.papers() + scenario.velocities()) {
			for (download in instance.downloads()) {
				if (download.identifier.orNull.isNullOrBlank()) {
					throw GradleException("Scenario '${scenario.getName()}' instance '${instance.getName()}' has a download with no identifier.")
				}
			}
		}
		for (velocity in scenario.velocities()) {
			if (velocity.servers().isEmpty()) {
				throw GradleException("Scenario '${scenario.getName()}' velocity '${velocity.getName()}' must define at least one backend server.")
			}
			val tryServers = velocity.tryServers.orNull ?: emptyList()
			val serverNames = velocity.servers().map { it.getName() }.toSet()
			if (tryServers.any { it !in serverNames }) {
				throw GradleException("Scenario '${scenario.getName()}' velocity '${velocity.getName()}' has tryServers entries that are not declared servers.")
			}
		}
	}
}
