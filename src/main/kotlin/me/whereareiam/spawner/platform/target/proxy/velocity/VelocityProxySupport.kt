package me.whereareiam.spawner.platform.target.proxy.velocity

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.config.scenario.SpawnerScenarioConfig
import me.whereareiam.spawner.installPlan
import me.whereareiam.spawner.model.FileInstallPlan
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.platform.RegisteredPlatformTasks
import me.whereareiam.spawner.platform.SpawnerPlatformSupport
import me.whereareiam.spawner.platform.TargetRole
import me.whereareiam.spawner.platform.requireScenarioDownloadIdentifiers
import me.whereareiam.spawner.platform.target.proxy.velocity.model.VelocityInstancePlan
import me.whereareiam.spawner.platform.target.proxy.velocity.model.VelocityServerPlan
import me.whereareiam.spawner.relativePath
import me.whereareiam.spawner.toTaskSuffix
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

internal class VelocityProxySupport : SpawnerPlatformSupport {
	private val target = VelocityProxyTarget()

	override val id: String = "velocity"
	override val role: TargetRole = TargetRole.PROXY

	override fun applyConventions(project: Project, config: SpawnerConfig) {
		config.velocity.port.convention(
			project.providers.gradleProperty("dev.velocity.port").map { it.toInt() }.orElse(25565)
		)
		config.velocity.version.convention(project.providers.gradleProperty("dev.velocity.version"))
		config.velocity.downloadProvider.convention(
			project.providers.gradleProperty("dev.velocity.download.provider").orElse("")
		)
		config.velocity.jvmArgs.convention(
			project.providers.gradleProperty("dev.velocity.jvmArgs")
				.map { it.split(" ").filter(String::isNotBlank) }
				.orElse(listOf("-Xms1G", "-Xmx1G"))
		)
		config.velocity.onlineMode.convention(
			project.providers.gradleProperty("dev.velocity.onlineMode").map { it.toBoolean() }.orElse(false)
		)
		config.velocity.forwardingMode.convention(
			project.providers.gradleProperty("dev.velocity.forwardingMode").orElse("none")
		)
		project.plugins.withId("com.github.johnrengelman.shadow") {
			val shadowJar = project.tasks.named("shadowJar", Jar::class.java)
			config.velocity.pluginJar.convention(shadowJar.flatMap { it.archiveFile })
		}
	}

	override fun registerStandalone(
		project: Project,
		config: SpawnerConfig,
		runtime: RuntimeSettings
	): RegisteredPlatformTasks {
		val velocityDir = runtime.serverDir.resolve(id)
		val installs = mutableListOf<FileInstallPlan>()
		if (config.velocity.pluginJar.isPresent)
			installs += installPlan(project, "plugins", config.velocity.pluginJar)
		if (!config.velocity.extraFiles.isEmpty) {
			val extraDir = config.velocity.extraFilesDir.orNull?.asFile
				?: throw GradleException("spawner.velocity.extraFilesDir is required when velocity.extraFiles is set.")
			installs += installPlan(project, relativePath(velocityDir, extraDir), config.velocity.extraFiles)
		}
		val plan = VelocityInstancePlan(
			name = id,
			directory = velocityDir,
			port = config.velocity.port.get(),
			version = config.velocity.version.orNull,
			downloadProvider = config.velocity.downloadProvider.orNull,
			jvmArgs = config.velocity.jvmArgs.get(),
			onlineMode = config.velocity.onlineMode.get(),
			forwardingMode = config.velocity.forwardingMode.get(),
			servers = listOf(VelocityServerPlan("lobby", "127.0.0.1:${config.paper.port.get()}")),
			tryServers = listOf("lobby"),
			rootOverlayDir = null,
			installs = installs,
			downloads = emptyList()
		)
		val tasks = target.registerTasks(project, runtime, plan, "")
		return RegisteredPlatformTasks(
			displayName = "Velocity",
			prepareTaskName = "prepareVelocity",
			runTaskName = "runVelocity",
			targetTasks = tasks
		)
	}

	override fun registerScenario(
		project: Project,
		scenario: SpawnerScenarioConfig,
		runtime: RuntimeSettings
	): List<RegisteredPlatformTasks> {
		val scenarioName = scenario.getName()
		val scenarioSuffix = scenarioName.toTaskSuffix()
		val scenarioDir = runtime.serverDir.resolve(scenarioName)
		return scenario.velocities().map { velocity ->
			val plan = VelocityInstancePlan(
				name = velocity.getName(),
				directory = scenarioDir.resolve(velocity.getName()),
				port = velocity.port.get(),
				version = velocity.version.orNull,
				downloadProvider = velocity.downloadProvider.orNull,
				jvmArgs = velocity.jvmArgs.get(),
				onlineMode = velocity.onlineMode.get(),
				forwardingMode = velocity.forwardingMode.get(),
				servers = velocity.servers().map { server ->
					VelocityServerPlan(server.getName(), server.address.orNull ?: "")
				},
				tryServers = velocity.tryServers.orNull?.takeIf { it.isNotEmpty() }
					?: velocity.servers().map { it.getName() },
				rootOverlayDir = velocity.rootOverlayDir.orNull?.asFile,
				installs = velocity.installs().map { install ->
					FileInstallPlan(install.files, install.into.orNull ?: "")
				},
				downloads = velocity.downloads().map { download ->
					DownloadPlan(
						provider = download.provider.get(),
						identifier = download.identifier.get(),
						version = download.version.orNull,
						versionType = download.versionType.get(),
						fileName = download.fileName.orNull,
						into = download.into.orNull ?: ""
					)
				}
			)
			val taskSuffix = "${scenarioSuffix}${plan.name.toTaskSuffix()}"
			val tasks = target.registerTasks(project, runtime, plan, taskSuffix)
			RegisteredPlatformTasks(
				displayName = "$scenarioName ${plan.name}",
				prepareTaskName = "prepare$taskSuffix",
				runTaskName = "run$taskSuffix",
				targetTasks = tasks
			)
		}
	}

	override fun validateScenario(scenario: SpawnerScenarioConfig) {
		for (velocity in scenario.velocities()) {
			requireScenarioDownloadIdentifiers(
				"${scenario.getName()} ${velocity.getName()}",
				velocity.downloads().map { it.identifier.orNull }
			)
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
