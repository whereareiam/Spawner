package me.whereareiam.spawner.target.proxy.velocity

import me.whereareiam.spawner.SpawnerConfig
import me.whereareiam.spawner.download.downloadFile
import me.whereareiam.spawner.download.resolveDownload
import me.whereareiam.spawner.file.execCommand
import me.whereareiam.spawner.file.installProviderJars
import me.whereareiam.spawner.isVelocityProxy
import me.whereareiam.spawner.target.SpawnerTarget
import me.whereareiam.spawner.target.TargetTasks
import me.whereareiam.spawner.task.RunServerTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

internal class VelocityProxyTarget : SpawnerTarget {
	override val id: String = "velocity"

	override fun isEnabled(config: SpawnerConfig): Boolean = isVelocityProxy(config)

	override fun registerTasks(project: Project, config: SpawnerConfig): TargetTasks {
		val velocityDir = config.serverDir.dir("velocity")
		val velocityJar = velocityDir.map { it.file("velocity.jar") }

		val downloadVelocity = project.tasks.register("downloadVelocity") {
			group = "devserver"
			description = "Download Velocity proxy jar for dev testing."
			outputs.file(velocityJar)
			onlyIf { isEnabled(config) }
			doLast {
				val jarFile = velocityJar.get().asFile
				if (jarFile.exists() && !config.forceDownload.get()) {
					project.logger.lifecycle("Velocity jar already present: ${jarFile.absolutePath}")
					return@doLast
				}
				val resolved = resolveDownload(
					config,
					"velocity",
					config.velocity.version.orNull,
					config.velocity.downloadProvider.orNull
				)
				project.logger.lifecycle("Downloading Velocity ${resolved.version} from ${resolved.url}")
				jarFile.parentFile.mkdirs()
				downloadFile(resolved.url, jarFile, config.userAgent.get())
			}
		}

		val installVelocityPlugin = project.tasks.register("installVelocityPlugin", Copy::class.java) {
			group = "devserver"
			description = "Install plugin into Velocity dev proxy."
			onlyIf { isEnabled(config) }
			doFirst {
				if (!config.velocity.pluginJar.isPresent) {
					throw GradleException("spawner.velocity.pluginJar is required to install the Velocity plugin.")
				}
			}
			from(config.velocity.pluginJar)
			into(velocityDir.map { it.dir("plugins").asFile })
		}

		val prepareVelocityDev = project.tasks.register("prepareVelocityDev") {
			group = "devserver"
			description = "Prepare Velocity dev proxy directory."
			dependsOn(downloadVelocity, installVelocityPlugin, config.providerJars)
			onlyIf { isEnabled(config) }
			doLast {
				val configText = """
					# Config version. Do not change this
					config-version = "2.6"

					# What port should the proxy be bound to? By default, we'll bind to all addresses on port 25577.
					bind = "0.0.0.0:${config.velocity.port.get()}"

					# What should be the MOTD? This gets displayed when the player adds your server to
					# their server list. Only MiniMessage format is accepted.
					motd = "<#09add3>Dev"

					# What should we display for the maximum number of players? (Velocity does not support a cap
					# on the number of players online.)
					show-max-players = 500

					# Should we authenticate players with Mojang? By default, this is on.
					online-mode = ${config.velocity.onlineMode.get()}

					# Should the proxy enforce the new public key security standard? By default, this is on.
					force-key-authentication = ${config.velocity.onlineMode.get()}

					# If client's ISP/AS sent from this proxy is different from the one from Mojang's
					# authentication server, the player is kicked. This disallows some VPN and proxy
					# connections but is a weak form of protection.
					prevent-client-proxy-connections = false

					# Should we forward IP addresses and other data to backend servers?
					# Available options:
					# - "none":        No forwarding will be done. All players will appear to be connecting
					#                  from the proxy and will have offline-mode UUIDs.
					# - "legacy":      Forward player IPs and UUIDs in a BungeeCord-compatible format. Use this
					#                  if you run servers using Minecraft 1.12 or lower.
					# - "bungeeguard": Forward player IPs and UUIDs in a format supported by the BungeeGuard
					#                  plugin. Use this if you run servers using Minecraft 1.12 or lower, and are
					#                  unable to implement network level firewalling (on a shared host).
					# - "modern":      Forward player IPs and UUIDs as part of the login process using
					#                  Velocity's native forwarding. Only applicable for Minecraft 1.13 or higher.
					player-info-forwarding-mode = "${config.velocity.forwardingMode.get()}"

					# If you are using modern or BungeeGuard IP forwarding, configure a file that contains a unique secret here.
					# The file is expected to be UTF-8 encoded and not empty.
					forwarding-secret-file = "forwarding.secret"

					# Announce whether or not your server supports Forge. If you run a modded server, we
					# suggest turning this on.
					#
					# If your network runs one modpack consistently, consider using ping-passthrough = "mods"
					# instead for a nicer display in the server list.
					announce-forge = false

					# If enabled (default is false) and the proxy is in online mode, Velocity will kick
					# any existing player who is online if a duplicate connection attempt is made.
					kick-existing-players = false

					# Should Velocity pass server list ping requests to a backend server?
					# Available options:
					# - "disabled":    No pass-through will be done. The velocity.toml and server-icon.png
					#                  will determine the initial server list ping response.
					# - "mods":        Passes only the mod list from your backend server into the response.
					#                  The first server in the try list (or forced host) with a mod list will be
					#                  used. If no backend servers can be contacted, Velocity won't display any
					#                  mod information.
					# - "description": Uses the description and mod list from the backend server. The first
					#                  server in the try (or forced host) list that responds is used for the
					#                  description and mod list.
					# - "all":         Uses the backend server's response as the proxy response. The Velocity
					#                  configuration is used if no servers could be contacted.
					ping-passthrough = "DISABLED"

					# If not enabled (default is true) player IP addresses will be replaced by <ip address withheld> in logs
					enable-player-address-logging = true

					[servers]
					# Configure your servers here. Each key represents the server's name, and the value
					# represents the IP address of the server to connect to.
					lobby = "127.0.0.1:${config.paper.port.get()}"

					# In what order we should try servers when a player logs in or is kicked from a server.
					try = ["lobby"]

					[forced-hosts]
					# Configure your forced hosts here.
				""".trimIndent()

				val configFile = velocityDir.get().file("velocity.toml").asFile
				configFile.parentFile.mkdirs()
				configFile.writeText(configText + System.lineSeparator())

				val pluginDataDir = velocityDir.get().dir("plugins")
					.dir(config.pluginDataDirName.get()).asFile
				installProviderJars(pluginDataDir, config)
			}
		}

		val runVelocityDev = project.tasks.register("runVelocityDev", RunServerTask::class.java) {
			group = "devserver"
			description = "Run the Velocity dev proxy."
			dependsOn(prepareVelocityDev)
			onlyIf { isEnabled(config) }
			workingDir.set(velocityDir)
			commandLine.set(project.provider {
				val jar = velocityJar.get().asFile
				listOf("java") + config.velocity.jvmArgs.get() + listOf("-jar", jar.name)
			})
		}

		return TargetTasks(prepare = prepareVelocityDev, run = runVelocityDev)
	}

	override fun startDetached(project: Project, config: SpawnerConfig) {
		val velocityDir = config.serverDir.dir("velocity")
		val velocityJarName = velocityDir.map { it.file("velocity.jar") }.get().asFile.name
		val velocityArgs = config.velocity.jvmArgs.get()
		val isWindows = System.getProperty("os.name").lowercase().contains("win")
		if (isWindows) {
			execCommand(
				velocityDir.get().asFile,
				listOf(
					"cmd", "/c", "start", "Velocity Dev", "/D", velocityDir.get().asFile.absolutePath,
					"java", *velocityArgs.toTypedArray(), "-jar", velocityJarName
				),
				false
			)
		} else {
			execCommand(
				velocityDir.get().asFile,
				listOf(
					"sh", "-c",
					"cd '${velocityDir.get().asFile.absolutePath}' && java ${velocityArgs.joinToString(" ")} -jar $velocityJarName &"
				),
				false
			)
		}
	}
}
