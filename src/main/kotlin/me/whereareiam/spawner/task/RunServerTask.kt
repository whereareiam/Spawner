package me.whereareiam.spawner.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

abstract class RunServerTask : DefaultTask() {
	@get:InputDirectory
	abstract val workingDir: DirectoryProperty

	@get:Input
	abstract val commandLine: ListProperty<String>

	@get:Input
	abstract val killTimeoutSeconds: Property<Long>

	init {
		killTimeoutSeconds.convention(5)
	}

	@TaskAction
	fun run() {
		val process = ProcessBuilder(commandLine.get())
			.directory(workingDir.get().asFile)
			.start()
		val shutdownHook = Thread(
			{ stopProcessTree(process, killTimeoutSeconds.get()) },
			"${name}-shutdown"
		)

		pipeToLogger(process.inputStream, logger::lifecycle)
		pipeToLogger(process.errorStream, logger::error)
		pipeInput(System.`in`, process.outputStream)
		Runtime.getRuntime().addShutdownHook(shutdownHook)

		try {
			val exitCode = process.waitFor()
			if (exitCode != 0) {
				throw GradleException("Server exited with code $exitCode")
			}
		} catch (_: InterruptedException) {
			stopProcessTree(process, killTimeoutSeconds.get())
			Thread.currentThread().interrupt()
		} finally {
			runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
		}
	}

	private fun pipeToLogger(input: InputStream, log: (String) -> Unit) {
		Thread(
			{
				input.bufferedReader().useLines { lines ->
					lines.forEach { log(it) }
				}
			},
			"${name}-out"
		).apply {
			isDaemon = true
			start()
		}
	}

	private fun pipeInput(input: InputStream, output: OutputStream) {
		Thread(
			{
				try {
					input.copyTo(output)
					output.flush()
				} catch (_: Exception) {
					// Ignore input errors during shutdown.
				}
			},
			"${name}-in"
		).apply {
			isDaemon = true
			start()
		}
	}

	private fun stopProcessTree(process: Process, timeoutSeconds: Long) {
		val handle = process.toHandle()
		val descendants = handle.descendants().toList()
		descendants.forEach { it.destroy() }
		process.destroy()
		val timeout = if (timeoutSeconds <= 0) 1 else timeoutSeconds
		if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
			descendants.forEach { if (it.isAlive) it.destroyForcibly() }
			if (process.isAlive) {
				process.destroyForcibly()
			}
		}
	}
}
