package me.whereareiam.spawner

import me.whereareiam.spawner.config.SpawnerConfig
import me.whereareiam.spawner.model.FileInstallPlan
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.target.TargetTasks
import me.whereareiam.spawner.target.proxy.bungeecord.BungeeCordProxyTarget
import me.whereareiam.spawner.target.proxy.bungeecord.model.BungeeCordInstancePlan
import me.whereareiam.spawner.target.proxy.bungeecord.model.BungeeCordServerPlan
import me.whereareiam.spawner.target.proxy.velocity.VelocityProxyTarget
import me.whereareiam.spawner.target.proxy.velocity.model.VelocityInstancePlan
import me.whereareiam.spawner.target.proxy.velocity.model.VelocityServerPlan
import me.whereareiam.spawner.target.server.paper.PaperServerTarget
import me.whereareiam.spawner.target.server.paper.model.PaperInstancePlan
import me.whereareiam.spawner.task.RunServerGroupTask
import org.gradle.api.Project

internal fun registerScenarioMode(project: Project, config: SpawnerConfig, runtime: RuntimeSettings) {
	val paperTarget = PaperServerTarget()
	val bungeecordTarget = BungeeCordProxyTarget()
	val velocityTarget = VelocityProxyTarget()

	for (scenario in config.scenarios.all()) {
		val scenarioName = scenario.getName()
		val scenarioSuffix = scenarioName.toTaskSuffix()
		val registered = mutableListOf<TargetTasks>()
		val groupedServerSpecs = mutableListOf<String>()
		val scenarioDir = runtime.serverDir.resolve(scenarioName)

		scenario.papers().forEach { paper ->
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
			val tasks = paperTarget.registerTasks(project, runtime, plan, "${scenarioSuffix}${plan.name.toTaskSuffix()}")
			registered += tasks
			groupedServerSpecs += tasks.serverSpec
			val runTaskName = "run${scenarioSuffix}${plan.name.toTaskSuffix()}"
			registerInstanceAliases(
				project,
				"$scenarioName ${plan.name}",
				"prepare${scenarioSuffix}${plan.name.toTaskSuffix()}",
				runTaskName,
				tasks
				)
			}

			scenario.bungeecords().forEach { bungeecord ->
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
				val tasks = bungeecordTarget.registerTasks(project, runtime, plan, "${scenarioSuffix}${plan.name.toTaskSuffix()}")
				registered += tasks
				groupedServerSpecs += tasks.serverSpec
				val runTaskName = "run${scenarioSuffix}${plan.name.toTaskSuffix()}"
				registerInstanceAliases(
					project,
					"$scenarioName ${plan.name}",
					"prepare${scenarioSuffix}${plan.name.toTaskSuffix()}",
					runTaskName,
					tasks
				)
			}

			scenario.velocities().forEach { velocity ->
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
			val tasks = velocityTarget.registerTasks(project, runtime, plan, "${scenarioSuffix}${plan.name.toTaskSuffix()}")
			registered += tasks
			groupedServerSpecs += tasks.serverSpec
			val runTaskName = "run${scenarioSuffix}${plan.name.toTaskSuffix()}"
			registerInstanceAliases(
				project,
				"$scenarioName ${plan.name}",
				"prepare${scenarioSuffix}${plan.name.toTaskSuffix()}",
				runTaskName,
				tasks
			)
		}

		val prepareTask = project.tasks.register("prepareDev$scenarioSuffix") {
			group = "devserver"
			description = "Prepare the ${scenarioName} development scenario."
			dependsOn(registered.map { it.prepare })
		}

		val groupedRunTaskName = "runDev$scenarioSuffix"
		project.tasks.register(groupedRunTaskName, RunServerGroupTask::class.java) {
			group = "devserver"
			description = "Run the ${scenarioName} development scenario."
			dependsOn(prepareTask)
			serverSpecs.set(groupedServerSpecs)
		}
	}
}
