package me.whereareiam.spawner.target.server.paper

import me.whereareiam.spawner.download.downloadFile
import me.whereareiam.spawner.download.resolveDownload
import me.whereareiam.spawner.download.installDownloads
import me.whereareiam.spawner.file.execCommand
import me.whereareiam.spawner.file.installFiles
import me.whereareiam.spawner.file.installOverlay
import me.whereareiam.spawner.file.updatePropertyFile
import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.model.RuntimeSettings
import me.whereareiam.spawner.target.SpawnerTarget
import me.whereareiam.spawner.target.TargetTasks
import me.whereareiam.spawner.task.RunServerGroupTask
import me.whereareiam.spawner.task.RunServerTask
import me.whereareiam.spawner.target.server.paper.model.PaperInstancePlan
import org.gradle.api.Project

internal class PaperServerTarget : SpawnerTarget<PaperInstancePlan> {
	override val id: String = "paper"

	override fun registerTasks(
		project: Project,
		runtime: RuntimeSettings,
		instance: PaperInstancePlan,
		taskSuffix: String
	): TargetTasks {
		val paperDir = instance.directory
		val paperJar = paperDir.resolve("paper.jar")
		val workingDirectory = project.layout.projectDirectory.dir(
			project.projectDir.toPath().relativize(paperDir.toPath()).toString()
		)

		val downloadPaper = project.tasks.register("downloadPaper$taskSuffix") {
			group = "devserver"
			description = "Download Paper server jar for ${instance.name}."
			outputs.file(paperJar)
			doLast {
				if (paperJar.exists() && !runtime.forceDownload) {
					project.logger.lifecycle("Paper jar already present: ${paperJar.absolutePath}")
					return@doLast
				}
				val resolved = resolveDownload(
					downloadProviders = runtime.downloadProviders,
					defaultProvider = runtime.defaultDownloadProvider,
					userAgent = runtime.userAgent,
					download = DownloadPlan(
						provider = instance.downloadProvider,
						identifier = "paper",
						version = instance.version,
						versionType = "release",
						fileName = "paper.jar"
					)
				)
				project.logger.lifecycle("Downloading Paper ${resolved.version} from ${resolved.url}")
				paperJar.parentFile.mkdirs()
				downloadFile(resolved.url, paperJar, runtime.userAgent)
			}
		}

		val preparePaperDev = project.tasks.register("preparePaperDev$taskSuffix") {
			group = "devserver"
			description = "Prepare Paper dev server ${instance.name}."
			dependsOn(downloadPaper)
			dependsOn(instance.installs.map { it.files })
			doLast {
				instance.rootOverlayDir?.let { installOverlay(it, paperDir) }

				if (instance.acceptEula) {
					val eula = paperDir.resolve("eula.txt")
					eula.parentFile.mkdirs()
					eula.writeText("eula=true${System.lineSeparator()}")
				}

				updatePropertyFile(
					paperDir.resolve("server.properties"),
					mapOf(
						"server-port" to instance.port.toString(),
						"online-mode" to instance.onlineMode.toString()
					)
				)
				updateSpigotBungeecordSetting(
					paperDir.resolve("spigot.yml"),
					instance.enableBungeecord
				)

				installFiles(paperDir, instance.installs)
				installDownloads(
					runtime = runtime,
					downloads = instance.downloads,
					baseDir = paperDir,
					cacheNamespace = "${instance.directory.parentFile.name}/${instance.name}",
					logger = project.logger
				)
			}
		}

		val runPaperDev = project.tasks.register("runPaperDev$taskSuffix", RunServerTask::class.java) {
			group = "devserver"
			description = "Run the Paper dev server ${instance.name}."
			dependsOn(preparePaperDev)
			workingDir.set(workingDirectory)
			commandLine.set(
				project.provider {
					listOf("java") + instance.jvmArgs + listOf("-jar", paperJar.name, "nogui")
				}
			)
		}

		return TargetTasks(
			prepare = preparePaperDev,
			run = runPaperDev,
			serverSpec = RunServerGroupTask.encode(
				name = instance.name,
				workingDir = instance.directory,
				commandLine = listOf("java") + instance.jvmArgs + listOf("-jar", "paper.jar", "nogui")
			)
		)
	}

	override fun startDetached(project: Project, instance: PaperInstancePlan) {
		val paperJarName = instance.directory.resolve("paper.jar").name
		val paperArgs = instance.jvmArgs
		val isWindows = System.getProperty("os.name").lowercase().contains("win")
		if (isWindows) {
			execCommand(
				instance.directory,
				listOf(
					"cmd", "/c", "start", "Paper Dev", "/D", instance.directory.absolutePath,
					"java", *paperArgs.toTypedArray(), "-jar", paperJarName, "nogui"
				),
				false
			)
		} else {
			execCommand(
				instance.directory,
				listOf(
					"sh", "-c",
					"cd '${instance.directory.absolutePath}' && java ${paperArgs.joinToString(" ")} -jar $paperJarName nogui &"
				),
				false
			)
		}
	}
}
