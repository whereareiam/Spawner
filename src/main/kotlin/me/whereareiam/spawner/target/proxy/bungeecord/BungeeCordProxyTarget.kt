package me.whereareiam.spawner.target.proxy.bungeecord

import me.whereareiam.spawner.download.downloadFile
import me.whereareiam.spawner.download.installDownloads
import me.whereareiam.spawner.download.resolveDownload
import me.whereareiam.spawner.file.execCommand
import me.whereareiam.spawner.file.installFiles
import me.whereareiam.spawner.file.installOverlay
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.target.SpawnerTarget
import me.whereareiam.spawner.target.TargetTasks
import me.whereareiam.spawner.target.proxy.bungeecord.model.BungeeCordInstancePlan
import me.whereareiam.spawner.task.RunServerGroupTask
import me.whereareiam.spawner.task.RunServerTask
import org.gradle.api.Project

internal class BungeeCordProxyTarget : SpawnerTarget<BungeeCordInstancePlan> {
	override val id: String = "bungeecord"

	override fun registerTasks(
		project: Project,
		runtime: RuntimeSettings,
		instance: BungeeCordInstancePlan,
		taskSuffix: String
	): TargetTasks {
		val proxyDir = instance.directory
		val proxyJar = proxyDir.resolve("BungeeCord.jar")
		val workingDirectory = project.layout.projectDirectory.dir(
			project.projectDir.toPath().relativize(proxyDir.toPath()).toString()
		)

		val downloadBungeeCord = project.tasks.register("downloadBungeeCord$taskSuffix") {
			group = "devserver"
			description = "Download BungeeCord proxy jar for ${instance.name}."
			outputs.file(proxyJar)
			doLast {
				if (proxyJar.exists() && !runtime.forceDownload) {
					project.logger.lifecycle("BungeeCord jar already present: ${proxyJar.absolutePath}")
					return@doLast
				}
				val resolved = resolveDownload(
					downloadProviders = runtime.downloadProviders,
					defaultProvider = runtime.defaultDownloadProvider,
					userAgent = runtime.userAgent,
					download = DownloadPlan(
						provider = instance.downloadProvider,
						identifier = "BungeeCord",
						version = instance.version,
						versionType = "release",
						fileName = "BungeeCord.jar"
					)
				)
				project.logger.lifecycle("Downloading BungeeCord ${resolved.version} from ${resolved.url}")
				proxyJar.parentFile.mkdirs()
				downloadFile(resolved.url, proxyJar, runtime.userAgent)
			}
		}

		val prepareBungeeCordDev = project.tasks.register("prepareBungeeCordDev$taskSuffix") {
			group = "devserver"
			description = "Prepare BungeeCord dev proxy ${instance.name}."
			dependsOn(downloadBungeeCord)
			dependsOn(instance.installs.map { it.files })
			doLast {
				instance.rootOverlayDir?.let { installOverlay(it, proxyDir) }

				val configFile = proxyDir.resolve("config.yml")
				configFile.parentFile.mkdirs()
				configFile.writeText(renderConfig(instance) + System.lineSeparator())

				installFiles(proxyDir, instance.installs)
				installDownloads(
					runtime = runtime,
					downloads = instance.downloads,
					baseDir = proxyDir,
					cacheNamespace = "${instance.directory.parentFile.name}/${instance.name}",
					logger = project.logger
				)
			}
		}

		val runBungeeCordDev = project.tasks.register("runBungeeCordDev$taskSuffix", RunServerTask::class.java) {
			group = "devserver"
			description = "Run the BungeeCord dev proxy ${instance.name}."
			dependsOn(prepareBungeeCordDev)
			workingDir.set(workingDirectory)
			commandLine.set(
				project.provider {
					listOf("java") + instance.jvmArgs + listOf("-jar", proxyJar.name)
				}
			)
		}

		return TargetTasks(
			prepare = prepareBungeeCordDev,
			run = runBungeeCordDev,
			serverSpec = RunServerGroupTask.encode(
				name = instance.name,
				workingDir = instance.directory,
				commandLine = listOf("java") + instance.jvmArgs + listOf("-jar", "BungeeCord.jar")
			)
		)
	}

	override fun startDetached(project: Project, instance: BungeeCordInstancePlan) {
		val jarName = instance.directory.resolve("BungeeCord.jar").name
		val proxyArgs = instance.jvmArgs
		val isWindows = System.getProperty("os.name").lowercase().contains("win")
		if (isWindows) {
			execCommand(
				instance.directory,
				listOf(
					"cmd", "/c", "start", "BungeeCord Dev", "/D", instance.directory.absolutePath,
					"java", *proxyArgs.toTypedArray(), "-jar", jarName
				),
				false
			)
		} else {
			execCommand(
				instance.directory,
				listOf(
					"sh", "-c",
					"cd '${instance.directory.absolutePath}' && java ${proxyArgs.joinToString(" ")} -jar $jarName &"
				),
				false
			)
		}
	}

	private fun renderConfig(instance: BungeeCordInstancePlan): String {
		val priorityLines = instance.tryServers.joinToString(System.lineSeparator()) { "  - $it" }
		val serverLines = instance.servers.joinToString(System.lineSeparator()) { server ->
			listOf(
				"  ${server.name}:",
				"    motd: '&1Just another BungeeCord - Forced Host'",
				"    address: ${server.address}",
				"    restricted: false"
			).joinToString(System.lineSeparator())
		}
		return listOf(
			"server_connect_timeout: 5000",
			"listeners:",
			"- query_port: ${instance.port}",
			"  motd: '&1Another Bungee server'",
			"  tab_list: GLOBAL_PING",
			"  query_enabled: false",
			"  proxy_protocol: false",
			"  forced_hosts: {}",
			"  ping_passthrough: false",
			"  priorities:",
			priorityLines,
			"  bind_local_address: true",
			"  host: 0.0.0.0:${instance.port}",
			"  max_players: 500",
			"  tab_size: 60",
			"  force_default_server: false",
			"remote_ping_cache: -1",
			"network_compression_threshold: 256",
			"permissions:",
			"  default:",
			"  - bungeecord.command.server",
			"  - bungeecord.command.list",
			"  admin:",
			"  - bungeecord.command.alert",
			"  - bungeecord.command.end",
			"  - bungeecord.command.ip",
			"  - bungeecord.command.reload",
			"timeout: 30000",
			"player_limit: -1",
			"prevent_proxy_connections: false",
			"ip_forward: ${instance.ipForward}",
			"groups:",
			"  md_5:",
			"  - admin",
			"remote_ping_timeout: 5000",
			"connection_throttle: 4000",
			"stats: ${java.util.UUID.randomUUID()}",
			"online_mode: ${instance.onlineMode}",
			"forge_support: false",
			"log_commands: false",
			"log_pings: true",
			"disabled_commands:",
			"- disabledcommandhere",
			"servers:",
			serverLines,
			"connection_throttle_limit: 3"
		).joinToString(System.lineSeparator())
	}
}
