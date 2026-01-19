package me.whereareiam.spawner.target.server.paper

import me.whereareiam.spawner.SpawnerConfig
import me.whereareiam.spawner.download.downloadFile
import me.whereareiam.spawner.download.resolveDownload
import me.whereareiam.spawner.file.execCommand
import me.whereareiam.spawner.file.installExtraFiles
import me.whereareiam.spawner.file.updatePropertyFile
import me.whereareiam.spawner.isPaperServer
import me.whereareiam.spawner.isVelocityProxy
import me.whereareiam.spawner.target.SpawnerTarget
import me.whereareiam.spawner.target.TargetTasks
import me.whereareiam.spawner.task.RunServerTask
import org.gradle.api.Project

internal class PaperServerTarget : SpawnerTarget {
	override val id: String = "paper"

	override fun isEnabled(config: SpawnerConfig): Boolean = isPaperServer(config)

	override fun registerTasks(project: Project, config: SpawnerConfig): TargetTasks {
		val paperDir = config.serverDir.dir("paper")
		val paperJar = paperDir.map { it.file("paper.jar") }

		val downloadPaper = project.tasks.register("downloadPaper") {
			group = "devserver"
			description = "Download Paper server jar for dev testing."
			outputs.file(paperJar)
			onlyIf { isEnabled(config) }
			doLast {
				val jarFile = paperJar.get().asFile
				if (jarFile.exists() && !config.forceDownload.get()) {
					project.logger.lifecycle("Paper jar already present: ${jarFile.absolutePath}")
					return@doLast
				}
				val resolved = resolveDownload(
					config,
					"paper",
					config.paper.version.orNull,
					config.paper.downloadProvider.orNull
				)
				project.logger.lifecycle("Downloading Paper ${resolved.version} from ${resolved.url}")
				jarFile.parentFile.mkdirs()
				downloadFile(resolved.url, jarFile, config.userAgent.get())
			}
		}

		val installPaperPlugin = project.tasks.register("installPaperPlugin") {
			group = "devserver"
			description = "Install plugin into Paper dev server."
			onlyIf { isEnabled(config) }
			doLast {
				if (!config.paper.pluginJar.isPresent) {
					project.logger.lifecycle("No Paper plugin jar configured; skipping install.")
					return@doLast
				}
				val pluginFile = config.paper.pluginJar.get().asFile
				val targetDir = paperDir.get().dir("plugins").asFile
				targetDir.mkdirs()
				java.nio.file.Files.copy(
					pluginFile.toPath(),
					targetDir.resolve(pluginFile.name).toPath(),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING
				)
			}
		}

		val preparePaperDev = project.tasks.register("preparePaperDev") {
			group = "devserver"
			description = "Prepare Paper dev server directory."
			dependsOn(downloadPaper, installPaperPlugin, config.paper.extraFiles)
			onlyIf { isEnabled(config) }
			doLast {
				if (config.paper.acceptEula.get()) {
					val eula = paperDir.get().file("eula.txt").asFile
					eula.parentFile.mkdirs()
					eula.writeText("eula=true${System.lineSeparator()}")
				}

				updatePropertyFile(
					paperDir.get().file("server.properties").asFile,
					mapOf(
						"server-port" to config.paper.port.get().toString(),
						"online-mode" to config.paper.onlineMode.get().toString()
					)
				)

				val forwardingMode = config.velocity.forwardingMode.get().lowercase()
				if (isVelocityProxy(config) && (forwardingMode == "legacy" || forwardingMode == "none")) {
					updateSpigotBungeecordSetting(
						paperDir.get().file("spigot.yml").asFile,
						forwardingMode == "legacy"
					)
				}

				val extraFiles = config.paper.extraFiles.files
				if (extraFiles.isNotEmpty()) {
					val extraDir = config.paper.extraFilesDir.orNull?.asFile
					if (extraDir == null) {
						project.logger.lifecycle("No extra files directory configured; skipping extra file install.")
						return@doLast
					}
					installExtraFiles(extraDir, extraFiles)
				}
			}
		}

		val runPaperDev = project.tasks.register("runPaperDev", RunServerTask::class.java) {
			group = "devserver"
			description = "Run the Paper dev server."
			dependsOn(preparePaperDev)
			onlyIf { isEnabled(config) }
			workingDir.set(paperDir)
			commandLine.set(project.provider {
				val jar = paperJar.get().asFile
				listOf("java") + config.paper.jvmArgs.get() + listOf("-jar", jar.name, "nogui")
			})
		}

		return TargetTasks(prepare = preparePaperDev, run = runPaperDev)
	}

	override fun startDetached(project: Project, config: SpawnerConfig) {
		val paperDir = config.serverDir.dir("paper")
		val paperJarName = paperDir.map { it.file("paper.jar") }.get().asFile.name
		val paperArgs = config.paper.jvmArgs.get()
		val isWindows = System.getProperty("os.name").lowercase().contains("win")
		if (isWindows) {
			execCommand(
				paperDir.get().asFile,
				listOf(
					"cmd", "/c", "start", "Paper Dev", "/D", paperDir.get().asFile.absolutePath,
					"java", *paperArgs.toTypedArray(), "-jar", paperJarName, "nogui"
				),
				false
			)
		} else {
			execCommand(
				paperDir.get().asFile,
				listOf(
					"sh", "-c",
					"cd '${paperDir.get().asFile.absolutePath}' && java ${paperArgs.joinToString(" ")} -jar $paperJarName nogui &"
				),
				false
			)
		}
	}
}

