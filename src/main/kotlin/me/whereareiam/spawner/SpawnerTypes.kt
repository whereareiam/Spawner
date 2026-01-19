package me.whereareiam.spawner

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

