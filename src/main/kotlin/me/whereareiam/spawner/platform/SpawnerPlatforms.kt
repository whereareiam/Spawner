package me.whereareiam.spawner.platform

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.platform.target.TargetTasks
import me.whereareiam.spawner.platform.target.proxy.bungeecord.BungeeCordProxySupport
import me.whereareiam.spawner.platform.target.proxy.velocity.VelocityProxySupport
import me.whereareiam.spawner.platform.target.server.paper.PaperServerSupport
import me.whereareiam.spawner.registerInstanceAliases
import me.whereareiam.spawner.task.RunServerGroupTask
import me.whereareiam.spawner.toTaskSuffix
import org.gradle.api.GradleException
import org.gradle.api.Project

internal fun builtInPlatforms(): List<SpawnerPlatformSupport> = listOf(
	PaperServerSupport(),
	BungeeCordProxySupport(),
	VelocityProxySupport()
)

internal fun validateConfiguredPlatforms(config: SpawnerConfig, platforms: List<SpawnerPlatformSupport>) {
	validateStandaloneType(config.serverType.orNull, TargetRole.SERVER, platforms)
	validateStandaloneType(config.proxyType.orNull, TargetRole.PROXY, platforms)
}

internal fun validateScenarioPlatforms(config: SpawnerConfig, platforms: List<SpawnerPlatformSupport>) {
	for (scenario in config.scenarios.all()) {
		for (platform in platforms)
			platform.validateScenario(scenario)
	}
}

internal fun registerStandalonePlatforms(
	project: Project,
	config: SpawnerConfig,
	runtime: RuntimeSettings,
	platforms: List<SpawnerPlatformSupport>
) {
	val registered = mutableListOf<TargetTasks>()
	val groupedServerSpecs = mutableListOf<String>()
	val selected = listOfNotNull(
		resolveStandalonePlatform(config.serverType.orNull, TargetRole.SERVER, platforms),
		resolveStandalonePlatform(config.proxyType.orNull, TargetRole.PROXY, platforms)
	)
	for (platform in selected) {
		val registration = platform.registerStandalone(project, config, runtime)
		registered += registration.targetTasks
		groupedServerSpecs += registration.targetTasks.serverSpec
		registerInstanceAliases(
			project,
			registration.displayName,
			registration.prepareTaskName,
			registration.runTaskName,
			registration.targetTasks
		)
	}

	val prepareDevServers = project.tasks.register("prepareDevServers") {
		group = "devserver"
		description = "Prepare dev servers for the current platform."
		dependsOn(registered.map { it.prepare })
	}

	project.tasks.register("runDevServers", RunServerGroupTask::class.java) {
		group = "devserver"
		description = "Run dev servers for the current platform."
		dependsOn(prepareDevServers)
		serverSpecs.set(groupedServerSpecs)
	}
}

internal fun registerScenarioPlatforms(
	project: Project,
	config: SpawnerConfig,
	runtime: RuntimeSettings,
	platforms: List<SpawnerPlatformSupport>
) {
	for (scenario in config.scenarios.all()) {
		val scenarioName = scenario.getName()
		val scenarioSuffix = scenarioName.toTaskSuffix()
		val registered = mutableListOf<TargetTasks>()
		val groupedServerSpecs = mutableListOf<String>()

		for (platform in platforms) {
			for (registration in platform.registerScenario(project, scenario, runtime)) {
				registered += registration.targetTasks
				groupedServerSpecs += registration.targetTasks.serverSpec
				registerInstanceAliases(
					project,
					registration.displayName,
					registration.prepareTaskName,
					registration.runTaskName,
					registration.targetTasks
				)
			}
		}

		val prepareTask = project.tasks.register("prepareDev$scenarioSuffix") {
			group = "devserver"
			description = "Prepare the ${scenarioName} development scenario."
			dependsOn(registered.map { it.prepare })
		}

		project.tasks.register("runDev$scenarioSuffix", RunServerGroupTask::class.java) {
			group = "devserver"
			description = "Run the ${scenarioName} development scenario."
			dependsOn(prepareTask)
			serverSpecs.set(groupedServerSpecs)
		}
	}
}

private fun resolveStandalonePlatform(
	configured: String?,
	role: TargetRole,
	platforms: List<SpawnerPlatformSupport>
): SpawnerPlatformSupport? {
	val type = configured?.lowercase() ?: "none"
	if (type == "none")
		return null
	return platforms.firstOrNull { it.role == role && it.id.equals(type, true) }
}

private fun validateStandaloneType(
	configured: String?,
	role: TargetRole,
	platforms: List<SpawnerPlatformSupport>
) {
	val type = configured?.lowercase() ?: "none"
	if (type == "none")
		return
	val supported = platforms
		.filter { it.role == role }
		.map { it.id }
		.sorted()
	if (type !in supported) {
		val available = (listOf("none") + supported).joinToString(", ")
		throw GradleException(
			"Unsupported dev.${role.propertyLabel}.type '$type'. Supported: $available"
		)
	}
}
