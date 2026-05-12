package me.whereareiam.spawner.platform.target.server.paper

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
import me.whereareiam.spawner.platform.target.server.paper.model.PaperInstancePlan
import me.whereareiam.spawner.relativePath
import me.whereareiam.spawner.toDisplayName
import me.whereareiam.spawner.toTaskSuffix
import org.gradle.api.GradleException
import org.gradle.api.Project

internal class PaperServerSupport : SpawnerPlatformSupport {
	private val target = PaperServerTarget()

	override val id: String = "paper"
	override val role: TargetRole = TargetRole.SERVER

	override fun applyConventions(project: Project, config: SpawnerConfig) {
		config.paper.port.convention(
			project.providers.gradleProperty("dev.paper.port").map { it.toInt() }.orElse(25566)
		)
		config.paper.version.convention(project.providers.gradleProperty("dev.paper.version"))
		config.paper.downloadProvider.convention(
			project.providers.gradleProperty("dev.paper.download.provider").orElse("")
		)
		config.paper.jvmArgs.convention(
			project.providers.gradleProperty("dev.paper.jvmArgs")
				.map { it.split(" ").filter(String::isNotBlank) }
				.orElse(listOf("-Xms1G", "-Xmx1G"))
		)
		config.paper.onlineMode.convention(
			project.providers.gradleProperty("dev.paper.onlineMode").map { it.toBoolean() }.orElse(false)
		)
	}

	override fun registerStandalone(
		project: Project,
		config: SpawnerConfig,
		runtime: RuntimeSettings
	): RegisteredPlatformTasks {
		val paperDir = runtime.serverDir.resolve(id)
		val installs = mutableListOf<FileInstallPlan>()
		if (config.paper.pluginJar.isPresent)
			installs += installPlan(project, "plugins", config.paper.pluginJar)
		if (!config.paper.extraFiles.isEmpty) {
			val extraDir = config.paper.extraFilesDir.orNull?.asFile
				?: throw GradleException("spawner.paper.extraFilesDir is required when paper.extraFiles is set.")
			installs += installPlan(project, relativePath(paperDir, extraDir), config.paper.extraFiles)
		}
		val plan = PaperInstancePlan(
			name = id,
			directory = paperDir,
			port = config.paper.port.get(),
			version = config.paper.version.orNull,
			downloadProvider = config.paper.downloadProvider.orNull,
			jvmArgs = config.paper.jvmArgs.get(),
			onlineMode = config.paper.onlineMode.get(),
			enableBungeecord = config.proxyType.orNull?.equals("bungeecord", true) == true ||
				(config.proxyType.orNull?.equals("velocity", true) == true &&
					config.velocity.forwardingMode.get().equals("legacy", true)),
			acceptEula = config.paper.acceptEula.get(),
			rootOverlayDir = null,
			installs = installs,
			downloads = emptyList()
		)
		val tasks = target.registerTasks(project, runtime, plan, "")
		return RegisteredPlatformTasks(
			displayName = "Paper",
			prepareTaskName = "preparePaper",
			runTaskName = "runPaper",
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
		return scenario.papers().map { paper ->
			val plan = PaperInstancePlan(
				name = paper.getName(),
				directory = scenarioDir.resolve(paper.getName()),
				port = paper.port.get(),
				version = paper.version.orNull,
				downloadProvider = paper.downloadProvider.orNull,
				jvmArgs = paper.jvmArgs.get(),
				onlineMode = paper.onlineMode.get(),
				enableBungeecord = scenario.bungeecords().isNotEmpty() || scenario.velocities().any {
					it.forwardingMode.get().equals("legacy", true)
				},
				acceptEula = paper.acceptEula.get(),
				rootOverlayDir = paper.rootOverlayDir.orNull?.asFile,
				installs = paper.installs().map { install ->
					FileInstallPlan(install.files, install.into.orNull ?: "")
				},
				downloads = paper.downloads().map { download ->
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
				displayName = "$scenarioName ${plan.name}".toDisplayName(),
				prepareTaskName = "prepare$taskSuffix",
				runTaskName = "run$taskSuffix",
				targetTasks = tasks
			)
		}
	}

	override fun validateScenario(scenario: SpawnerScenarioConfig) {
		for (paper in scenario.papers()) {
			requireScenarioDownloadIdentifiers(
				"${scenario.getName()} ${paper.getName()}",
				paper.downloads().map { it.identifier.orNull }
			)
		}
	}
}
