package me.whereareiam.spawner

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.model.FileInstallPlan
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.target.TargetTasks
import me.whereareiam.spawner.target.proxy.velocity.VelocityProxyTarget
import me.whereareiam.spawner.target.proxy.velocity.model.VelocityInstancePlan
import me.whereareiam.spawner.target.proxy.velocity.model.VelocityServerPlan
import me.whereareiam.spawner.target.server.paper.PaperServerTarget
import me.whereareiam.spawner.target.server.paper.model.PaperInstancePlan
import me.whereareiam.spawner.task.RunServerGroupTask
import org.gradle.api.GradleException
import org.gradle.api.Project

internal fun registerStandaloneMode(project: Project, config: SpawnerConfig, runtime: RuntimeSettings) {
	val paperTarget = PaperServerTarget()
	val velocityTarget = VelocityProxyTarget()
	val registered = mutableListOf<TargetTasks>()
	val groupedServerSpecs = mutableListOf<String>()

	if (isPaperServer(config)) {
		val paperDir = runtime.serverDir.resolve("paper")
		val enableBungeecord = isVelocityProxy(config) && config.velocity.forwardingMode.get().equals("legacy", true)
		val installs = mutableListOf<FileInstallPlan>()
		if (config.paper.pluginJar.isPresent) {
			installs += installPlan(project, "plugins", config.paper.pluginJar)
		}
		if (!config.paper.extraFiles.isEmpty) {
			val extraDir = config.paper.extraFilesDir.orNull?.asFile
				?: throw GradleException("spawner.paper.extraFilesDir is required when paper.extraFiles is set.")
			installs += installPlan(project, relativePath(paperDir, extraDir), config.paper.extraFiles)
		}

		val plan = PaperInstancePlan(
			name = "paper",
			directory = paperDir,
			port = config.paper.port.get(),
			version = config.paper.version.orNull,
			downloadProvider = config.paper.downloadProvider.orNull,
			jvmArgs = config.paper.jvmArgs.get(),
			onlineMode = config.paper.onlineMode.get(),
			enableBungeecord = enableBungeecord,
			acceptEula = config.paper.acceptEula.get(),
			rootOverlayDir = null,
			installs = installs,
			downloads = emptyList()
		)
		val tasks = paperTarget.registerTasks(project, runtime, plan, "")
		registered += tasks
		groupedServerSpecs += tasks.serverSpec
		registerInstanceAliases(project, "Paper", "preparePaper", "runPaper", tasks)
	}

	if (isVelocityProxy(config)) {
		val velocityDir = runtime.serverDir.resolve("velocity")
		val installs = mutableListOf<FileInstallPlan>()
		if (config.velocity.pluginJar.isPresent) {
			installs += installPlan(project, "plugins", config.velocity.pluginJar)
		}
		if (!config.velocity.extraFiles.isEmpty) {
			val extraDir = config.velocity.extraFilesDir.orNull?.asFile
				?: throw GradleException("spawner.velocity.extraFilesDir is required when velocity.extraFiles is set.")
			installs += installPlan(project, relativePath(velocityDir, extraDir), config.velocity.extraFiles)
		}

		val plan = VelocityInstancePlan(
			name = "velocity",
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
		val tasks = velocityTarget.registerTasks(project, runtime, plan, "")
		registered += tasks
		groupedServerSpecs += tasks.serverSpec
		registerInstanceAliases(project, "Velocity", "prepareVelocity", "runVelocity", tasks)
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
