package me.whereareiam.spawner.platform.target.proxy.velocity

import me.whereareiam.spawner.download.downloadFile
import me.whereareiam.spawner.download.installDownloads
import me.whereareiam.spawner.download.resolveDownload
import me.whereareiam.spawner.file.execCommand
import me.whereareiam.spawner.file.installFiles
import me.whereareiam.spawner.file.installOverlay
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.platform.target.SpawnerTarget
import me.whereareiam.spawner.platform.target.TargetTasks
import me.whereareiam.spawner.platform.target.proxy.velocity.model.VelocityInstancePlan
import me.whereareiam.spawner.task.RunServerGroupTask
import me.whereareiam.spawner.task.RunServerTask
import org.gradle.api.Project

internal class VelocityProxyTarget : SpawnerTarget<VelocityInstancePlan> {
	override val id: String = "velocity"

	override fun registerTasks(
		project: Project,
		runtime: RuntimeSettings,
		instance: VelocityInstancePlan,
		taskSuffix: String
	): TargetTasks {
		val velocityDir = instance.directory
		val velocityJar = velocityDir.resolve("velocity.jar")
		val workingDirectory = project.layout.projectDirectory.dir(
			project.projectDir.toPath().relativize(velocityDir.toPath()).toString()
		)

		val downloadVelocity = project.tasks.register("downloadVelocity$taskSuffix") {
			group = "devserver"
			description = "Download Velocity proxy jar for ${instance.name}."
			outputs.file(velocityJar)
			doLast {
				if (velocityJar.exists() && !runtime.forceDownload) {
					project.logger.lifecycle("Velocity jar already present: ${velocityJar.absolutePath}")
					return@doLast
				}
				val resolved = resolveDownload(
					downloadProviders = runtime.downloadProviders,
					defaultProvider = runtime.defaultDownloadProvider,
					userAgent = runtime.userAgent,
					download = DownloadPlan(
						provider = instance.downloadProvider,
						identifier = "velocity",
						version = instance.version,
						versionType = "release",
						fileName = "velocity.jar"
					)
				)
				project.logger.lifecycle("Downloading Velocity ${resolved.version} from ${resolved.url}")
				velocityJar.parentFile.mkdirs()
				downloadFile(resolved.url, velocityJar, runtime.userAgent)
			}
		}

		val prepareVelocityDev = project.tasks.register("prepareVelocityDev$taskSuffix") {
			group = "devserver"
			description = "Prepare Velocity dev proxy ${instance.name}."
			dependsOn(downloadVelocity)
			dependsOn(instance.installs.map { it.files })
			doLast {
				instance.rootOverlayDir?.let { installOverlay(it, velocityDir) }

				val configText = renderVelocityConfig(instance)
				val configFile = velocityDir.resolve("velocity.toml")
				configFile.parentFile.mkdirs()
				configFile.writeText(configText + System.lineSeparator())

				installFiles(velocityDir, instance.installs)
				installDownloads(
					runtime = runtime,
					downloads = instance.downloads,
					baseDir = velocityDir,
					cacheNamespace = "${instance.directory.parentFile.name}/${instance.name}",
					logger = project.logger
				)
			}
		}

		val runVelocityDev = project.tasks.register("runVelocityDev$taskSuffix", RunServerTask::class.java) {
			group = "devserver"
			description = "Run the Velocity dev proxy ${instance.name}."
			dependsOn(prepareVelocityDev)
			workingDir.set(workingDirectory)
			commandLine.set(
				project.provider {
					listOf("java") + instance.jvmArgs + listOf("-jar", velocityJar.name)
				}
			)
		}

		return TargetTasks(
			prepare = prepareVelocityDev,
			run = runVelocityDev,
			serverSpec = RunServerGroupTask.encode(
				name = instance.name,
				workingDir = instance.directory,
				commandLine = listOf("java") + instance.jvmArgs + listOf("-jar", "velocity.jar")
			)
		)
	}

	override fun startDetached(project: Project, instance: VelocityInstancePlan) {
		val velocityJarName = instance.directory.resolve("velocity.jar").name
		val velocityArgs = instance.jvmArgs
		val isWindows = System.getProperty("os.name").lowercase().contains("win")
		if (isWindows) {
			execCommand(
				instance.directory,
				listOf(
					"cmd", "/c", "start", "Velocity Dev", "/D", instance.directory.absolutePath,
					"java", *velocityArgs.toTypedArray(), "-jar", velocityJarName
				),
				false
			)
		} else {
			execCommand(
				instance.directory,
				listOf(
					"sh", "-c",
					"cd '${instance.directory.absolutePath}' && java ${velocityArgs.joinToString(" ")} -jar $velocityJarName &"
				),
				false
			)
		}
	}

	private fun renderVelocityConfig(instance: VelocityInstancePlan): String {
		val serverLines = instance.servers.joinToString(System.lineSeparator()) { server ->
			"\t${server.name} = \"${server.address}\""
		}
		val tryLines = instance.tryServers.joinToString(", ") { "\"$it\"" }

		return listOf(
			"# Config version. Do not change this",
			"config-version = \"2.6\"",
			"",
			"# What port should the proxy be bound to? By default, we'll bind to all addresses on port 25577.",
			"bind = \"0.0.0.0:${instance.port}\"",
			"",
			"# What should be the MOTD? This gets displayed when the player adds your server to",
			"# their server list. Only MiniMessage format is accepted.",
			"motd = \"<#09add3>Dev\"",
			"",
			"# What should we display for the maximum number of players? (Velocity does not support a cap",
			"# on the number of players online.)",
			"show-max-players = 500",
			"",
			"# Should we authenticate players with Mojang? By default, this is on.",
			"online-mode = ${instance.onlineMode}",
			"",
			"# Should the proxy enforce the new public key security standard? By default, this is on.",
			"force-key-authentication = ${instance.onlineMode}",
			"",
			"# If client's ISP/AS sent from this proxy is different from the one from Mojang's",
			"# authentication server, the player is kicked. This disallows some VPN and proxy",
			"# connections but is a weak form of protection.",
			"prevent-client-proxy-connections = false",
			"",
			"# Should we forward IP addresses and other data to backend servers?",
			"player-info-forwarding-mode = \"${instance.forwardingMode}\"",
			"",
			"# If you are using modern or BungeeGuard IP forwarding, configure a file that contains a unique secret here.",
			"# The file is expected to be UTF-8 encoded and not empty.",
			"forwarding-secret-file = \"forwarding.secret\"",
			"",
			"announce-forge = false",
			"kick-existing-players = false",
			"ping-passthrough = \"DISABLED\"",
			"enable-player-address-logging = true",
			"",
			"[servers]",
			serverLines,
			"",
			"try = [$tryLines]",
			"",
			"[forced-hosts]"
		).joinToString(System.lineSeparator())
	}
}
