package me.whereareiam.spawner.platform.target.proxy.bungeecord

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
import me.whereareiam.spawner.platform.target.proxy.bungeecord.model.BungeeCordInstancePlan
import me.whereareiam.spawner.platform.target.proxy.bungeecord.model.BungeeCordServerPlan
import me.whereareiam.spawner.relativePath
import me.whereareiam.spawner.toTaskSuffix
import org.gradle.api.GradleException
import org.gradle.api.Project

internal class BungeeCordProxySupport : SpawnerPlatformSupport {
	private val target = BungeeCordProxyTarget()

	override val id: String = "bungeecord"
	override val role: TargetRole = TargetRole.PROXY

	override fun applyConventions(project: Project, config: SpawnerConfig) {
		config.bungeecord.port.convention(
			project.providers.gradleProperty("dev.bungeecord.port").map { it.toInt() }.orElse(25565)
		)
		config.bungeecord.version.convention(project.providers.gradleProperty("dev.bungeecord.version"))
		config.bungeecord.downloadProvider.convention(
			project.providers.gradleProperty("dev.bungeecord.download.provider").orElse("spigot-jenkins")
		)
		config.bungeecord.jvmArgs.convention(
			project.providers.gradleProperty("dev.bungeecord.jvmArgs")
				.map { it.split(" ").filter(String::isNotBlank) }
				.orElse(listOf("-Xms1G", "-Xmx1G"))
		)
		config.bungeecord.onlineMode.convention(
			project.providers.gradleProperty("dev.bungeecord.onlineMode").map { it.toBoolean() }.orElse(false)
		)
		config.bungeecord.ipForward.convention(
			project.providers.gradleProperty("dev.bungeecord.ipForward").map { it.toBoolean() }.orElse(true)
		)
	}

	override fun registerStandalone(
		project: Project,
		config: SpawnerConfig,
		runtime: RuntimeSettings
	): RegisteredPlatformTasks {
		val proxyDir = runtime.serverDir.resolve(id)
		val installs = mutableListOf<FileInstallPlan>()
		if (config.bungeecord.pluginJar.isPresent)
			installs += installPlan(project, "plugins", config.bungeecord.pluginJar)
		if (!config.bungeecord.extraFiles.isEmpty) {
			val extraDir = config.bungeecord.extraFilesDir.orNull?.asFile
				?: throw GradleException("spawner.bungeecord.extraFilesDir is required when bungeecord.extraFiles is set.")
			installs += installPlan(project, relativePath(proxyDir, extraDir), config.bungeecord.extraFiles)
		}
		val plan = BungeeCordInstancePlan(
			name = id,
			directory = proxyDir,
			port = config.bungeecord.port.get(),
			version = config.bungeecord.version.orNull,
			downloadProvider = config.bungeecord.downloadProvider.orNull,
			jvmArgs = config.bungeecord.jvmArgs.get(),
			onlineMode = config.bungeecord.onlineMode.get(),
			ipForward = config.bungeecord.ipForward.get(),
			servers = listOf(BungeeCordServerPlan("lobby", "127.0.0.1:${config.paper.port.get()}")),
			tryServers = listOf("lobby"),
			rootOverlayDir = null,
			installs = installs,
			downloads = emptyList()
		)
		val tasks = target.registerTasks(project, runtime, plan, "")
		return RegisteredPlatformTasks(
			displayName = "BungeeCord",
			prepareTaskName = "prepareBungeeCord",
			runTaskName = "runBungeeCord",
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
		return scenario.bungeecords().map { bungeecord ->
			val plan = BungeeCordInstancePlan(
				name = bungeecord.getName(),
				directory = scenarioDir.resolve(bungeecord.getName()),
				port = bungeecord.port.get(),
				version = bungeecord.version.orNull,
				downloadProvider = bungeecord.downloadProvider.orNull,
				jvmArgs = bungeecord.jvmArgs.get(),
				onlineMode = bungeecord.onlineMode.get(),
				ipForward = bungeecord.ipForward.get(),
				servers = bungeecord.servers().map { server ->
					BungeeCordServerPlan(server.getName(), server.address.orNull ?: "")
				},
				tryServers = bungeecord.tryServers.orNull?.takeIf { it.isNotEmpty() }
					?: bungeecord.servers().map { it.getName() },
				rootOverlayDir = bungeecord.rootOverlayDir.orNull?.asFile,
				installs = bungeecord.installs().map { install ->
					FileInstallPlan(install.files, install.into.orNull ?: "")
				},
				downloads = bungeecord.downloads().map { download ->
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
		for (bungeecord in scenario.bungeecords()) {
			requireScenarioDownloadIdentifiers(
				"${scenario.getName()} ${bungeecord.getName()}",
				bungeecord.downloads().map { it.identifier.orNull }
			)
			if (bungeecord.servers().isEmpty()) {
				throw GradleException("Scenario '${scenario.getName()}' bungeecord '${bungeecord.getName()}' must define at least one backend server.")
			}
			val tryServers = bungeecord.tryServers.orNull ?: emptyList()
			val serverNames = bungeecord.servers().map { it.getName() }.toSet()
			if (tryServers.any { it !in serverNames }) {
				throw GradleException("Scenario '${scenario.getName()}' bungeecord '${bungeecord.getName()}' has tryServers entries that are not declared servers.")
			}
		}
	}
}
