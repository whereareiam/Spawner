package me.whereareiam.spawner

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SpawnerPluginTest {
	@TempDir
	lateinit var projectDir: Path

	@Test
	fun `registers named scenario tasks`() {
		writeProject(
			buildScript = """
				import me.whereareiam.spawner.config.SpawnerConfig

				plugins {
				    id("me.whereareiam.spawner")
				}

				extensions.configure<SpawnerConfig>("spawner") {
				    scenarios.register("sync") {
				        velocity("proxy") {
				            server("paper-a", "127.0.0.1:25566")
				            tryServers("paper-a")
				        }
				    }
				}
			""".trimIndent()
		)

		val result = gradle("help", "--task", "prepareDevSync")

		assertTrue(result.output.contains("Path"))
		assertTrue(result.output.contains(":prepareDevSync"))
	}

	@Test
	fun `prepares multi backend scenario with overlays and installs`() {
		writeProject(
			buildScript = """
				import me.whereareiam.spawner.config.SpawnerConfig
				import me.whereareiam.spawner.download.DownloadProvider
				import me.whereareiam.spawner.model.download.DownloadPlan
				import me.whereareiam.spawner.model.download.ResolvedDownload

				class FakeDownloadProvider : DownloadProvider {
				    override fun resolve(download: DownloadPlan, userAgent: String): ResolvedDownload {
				        return ResolvedDownload("https://example.invalid/channelizer.jar", "test", "channelizer.jar")
				    }
				}

				plugins {
				    id("me.whereareiam.spawner")
				}

				extensions.configure<SpawnerConfig>("spawner") {
				    registerDownloadProvider("fake", FakeDownloadProvider())
				    scenarios.register("sync") {
				        velocity("proxy") {
				            port.set(25565)
				            forwardingMode.set("legacy")
				            rootOverlayDir.set(layout.projectDirectory.dir("overlays/proxy"))
				            server("paper-a", "127.0.0.1:25566")
				            server("paper-b", "127.0.0.1:25567")
				            tryServers("paper-a", "paper-b")
				            install {
				                from(layout.projectDirectory.file("artifacts/proxy-plugin.jar"))
				                into("plugins")
				            }
				            install {
				                from(layout.projectDirectory.file("artifacts/channelizer.jar"))
				                into("plugins/socialismus/modules")
				            }
				            download {
				                provider.set("fake")
				                identifier.set("channelizer")
				                into("plugins/downloaded")
				            }
				        }

				        paper("paper-a") {
				            port.set(25566)
				            rootOverlayDir.set(layout.projectDirectory.dir("overlays/paper-a"))
				            install {
				                from(layout.projectDirectory.file("artifacts/paper-plugin.jar"))
				                into("plugins")
				            }
				            install {
				                from(layout.projectDirectory.file("artifacts/channelizer.jar"))
				                into("plugins/Socialismus/modules")
				            }
				        }

				        paper("paper-b") {
				            port.set(25567)
				            rootOverlayDir.set(layout.projectDirectory.dir("overlays/paper-b"))
				            install {
				                from(layout.projectDirectory.file("artifacts/paper-plugin.jar"))
				                into("plugins")
				            }
				        }
				    }
				}
			""".trimIndent()
		)

		writeFile("artifacts/proxy-plugin.jar", "proxy")
		writeFile("artifacts/paper-plugin.jar", "paper")
		writeFile("artifacts/channelizer.jar", "channelizer")
		writeFile("build/spawner-downloads/sync/proxy/channelizer.jar", "cached-channelizer")
		writeFile("server/sync/proxy/velocity.jar", "velocity-binary")
		writeFile("server/sync/paper-a/paper.jar", "paper-a-binary")
		writeFile("server/sync/paper-b/paper.jar", "paper-b-binary")
		writeFile("overlays/proxy/plugins/socialismus/chats/settings.yml", "proxy-overlay: true\n")
		writeFile("overlays/paper-a/plugins/Socialismus/chats/settings.yml", "paper-a-overlay: true\n")
		writeFile("overlays/paper-b/plugins/Socialismus/chats/settings.yml", "paper-b-overlay: true\n")

		val result = gradle("prepareDevSync")

		assertEquals(TaskOutcome.SUCCESS, result.task(":prepareDevSync")?.outcome)
		assertTrue(path("server/sync/proxy/plugins/proxy-plugin.jar").toFile().exists())
		assertTrue(path("server/sync/proxy/plugins/socialismus/modules/channelizer.jar").toFile().exists())
		assertTrue(path("server/sync/proxy/plugins/downloaded/channelizer.jar").toFile().exists())
		assertTrue(path("server/sync/proxy/plugins/downloaded/channelizer.jar").readText().contains("cached-channelizer"))
		assertTrue(path("server/sync/paper-a/plugins/paper-plugin.jar").toFile().exists())
		assertTrue(path("server/sync/paper-a/plugins/Socialismus/modules/channelizer.jar").toFile().exists())
		assertTrue(path("server/sync/paper-b/plugins/paper-plugin.jar").toFile().exists())
		assertTrue(path("server/sync/proxy/plugins/socialismus/chats/settings.yml").readText().contains("proxy-overlay"))
		assertTrue(path("server/sync/paper-a/plugins/Socialismus/chats/settings.yml").readText().contains("paper-a-overlay"))
		assertTrue(path("server/sync/paper-b/plugins/Socialismus/chats/settings.yml").readText().contains("paper-b-overlay"))

		val velocityToml = path("server/sync/proxy/velocity.toml").readText()
		assertTrue(velocityToml.contains("""paper-a = "127.0.0.1:25566""""))
		assertTrue(velocityToml.contains("""paper-b = "127.0.0.1:25567""""))
		assertTrue(velocityToml.contains("""try = ["paper-a", "paper-b"]"""))
		assertTrue(path("server/sync/paper-a/spigot.yml").readText().contains("bungeecord: true"))
	}

	@Test
	fun `prepares standalone dev servers`() {
		writeProject(
			buildScript = """
				import me.whereareiam.spawner.config.SpawnerConfig

				plugins {
				    id("me.whereareiam.spawner")
				}

				extensions.configure<SpawnerConfig>("spawner") {
				    serverType.set("paper")
				    proxyType.set("velocity")
				    paper.port.set(25566)
				    velocity.port.set(25565)
				    velocity.forwardingMode.set("legacy")
				    velocity.pluginJar.set(layout.projectDirectory.file("artifacts/velocity-plugin.jar"))
				    velocity.extraFiles.from(layout.projectDirectory.file("artifacts/provider.jar"))
				    velocity.extraFilesDir.set(serverDir.dir("velocity").map { it.dir("plugins/providers") })
				}
			""".trimIndent()
		)

		writeFile("artifacts/velocity-plugin.jar", "velocity-plugin")
		writeFile("artifacts/provider.jar", "provider")
		writeFile("server/velocity/velocity.jar", "velocity-binary")
		writeFile("server/paper/paper.jar", "paper-binary")

		val result = gradle("prepareDevServers")

		assertEquals(TaskOutcome.SUCCESS, result.task(":prepareDevServers")?.outcome)
		assertTrue(path("server/velocity/plugins/velocity-plugin.jar").toFile().exists())
		assertTrue(path("server/velocity/plugins/providers/provider.jar").toFile().exists())
		assertTrue(path("server/paper/spigot.yml").readText().contains("bungeecord: true"))
		assertTrue(path("server/velocity/velocity.toml").readText().contains("""lobby = "127.0.0.1:25566""""))
	}

	@Test
	fun `does not register intellij run configuration tasks`() {
		writeProject(
			buildScript = """
				import me.whereareiam.spawner.config.SpawnerConfig

				plugins {
				    id("me.whereareiam.spawner")
				}

				extensions.configure<SpawnerConfig>("spawner") {
				    scenarios.register("sync") {
				        velocity("proxy") {
				            server("paper-a", "127.0.0.1:25566")
				            tryServers("paper-a")
				        }
				        paper("paper-a") {
				            port.set(25566)
				        }
				    }
				}
			""".trimIndent()
		)

		val result = gradle("tasks", "--all")

		assertTrue(!result.output.contains("generateIdeRunConfigurations"))
		assertTrue(!result.output.contains("generateIntellijRunConfigurations"))
	}

	private fun gradle(vararg arguments: String) = GradleRunner.create()
		.withProjectDir(projectDir.toFile())
		.withArguments(*arguments, "--stacktrace")
		.withPluginClasspath()
		.build()

	private fun writeProject(buildScript: String) {
		writeFile("settings.gradle.kts", "rootProject.name = \"spawner-test\"\n")
		writeFile("build.gradle.kts", buildScript)
	}

	private fun writeFile(relativePath: String, content: String) {
		val file = path(relativePath)
		file.parent?.createDirectories()
		file.writeText(content)
	}

	private fun path(relativePath: String): Path = projectDir.resolve(relativePath)
}
