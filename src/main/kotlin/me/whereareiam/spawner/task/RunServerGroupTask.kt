package me.whereareiam.spawner.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

abstract class RunServerGroupTask : DefaultTask() {
	@get:Input
	abstract val serverSpecs: ListProperty<String>

	@get:Input
	abstract val killTimeoutSeconds: Property<Long>

	init {
		killTimeoutSeconds.convention(5)
	}

	@TaskAction
	fun run() {
		val specs = serverSpecs.get().map(::decodeSpec)
		val processes = specs.map { spec ->
			val process = ProcessBuilder(spec.commandLine)
				.directory(spec.workingDir)
				.start()
			pipeToLogger(spec.name, process.inputStream, logger::lifecycle)
			pipeToLogger(spec.name, process.errorStream, logger::error)
			process
		}
		val writers = processes.map { process ->
			BufferedWriter(OutputStreamWriter(process.outputStream, Charsets.UTF_8))
		}

		val shutdownHook = Thread({ stopAll(processes) }, "${name}-shutdown")
		Runtime.getRuntime().addShutdownHook(shutdownHook)

		try {
			startInputRouter(specs, processes, writers)
			waitForProcesses(specs, processes)
		} catch (_: InterruptedException) {
			stopAll(processes)
			Thread.currentThread().interrupt()
		} finally {
			runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
		}
	}

	private fun waitForProcesses(specs: List<ServerProcessSpec>, processes: List<Process>) {
		while (true) {
			for ((index, process) in processes.withIndex()) {
				if (!process.isAlive) {
					val exitCode = process.exitValue()
					if (exitCode != 0) {
						stopAll(processes)
						throw GradleException("Server '${specs[index].name}' exited with code $exitCode")
					}
					stopAll(processes.filterIndexed { processIndex, _ -> processIndex != index })
					logger.lifecycle("Server '${specs[index].name}' stopped. Ending grouped run.")
					return
				}
			}
			Thread.sleep(1000)
		}
	}

	private fun startInputRouter(
		specs: List<ServerProcessSpec>,
		processes: List<Process>,
		writers: List<BufferedWriter>
	) {
		Thread(
			{
				val names = specs.map { it.name }
				System.`in`.bufferedReader().useLines { lines ->
					lines.forEach { line ->
						handleInput(line, processes, writers, names)
					}
				}
			},
			"${name}-stdin-router"
		).apply {
			isDaemon = true
			start()
		}
	}

	private fun handleInput(
		line: String,
		processes: List<Process>,
		writers: List<BufferedWriter>,
		names: List<String>
	) {
		val trimmed = line.trim()
		if (trimmed.isEmpty()) return
		if (trimmed == ":help") {
			logger.lifecycle("Commands: :help, :list, all: <command>, <server>: <command>")
			if (names.size == 1) {
				logger.lifecycle("With a single server, plain input is sent directly to '${names.first()}'.")
			}
			return
		}
		if (trimmed == ":list") {
			logger.lifecycle("Available servers: ${names.joinToString(", ")}")
			return
		}

		val colonIndex = trimmed.indexOf(':')
		if (colonIndex > 0) {
			val target = trimmed.substring(0, colonIndex).trim()
			val command = trimmed.substring(colonIndex + 1).trim()
			if (command.isEmpty()) {
				logger.lifecycle("No command provided for target '$target'.")
				return
			}
			if (target.equals("all", true)) {
				writers.forEachIndexed { index, writer ->
					if (processes[index].isAlive) {
						sendCommand(names[index], command, writer)
					}
				}
				return
			}
			val serverIndex = names.indexOfFirst { it.equals(target, true) }
			if (serverIndex == -1) {
				logger.lifecycle("Unknown server '$target'. Use :list to see available servers.")
				return
			}
			if (!processes[serverIndex].isAlive) {
				logger.lifecycle("Server '${names[serverIndex]}' is not running.")
				return
			}
			sendCommand(names[serverIndex], command, writers[serverIndex])
			return
		}

		if (names.size == 1) {
			if (processes[0].isAlive) {
				sendCommand(names[0], trimmed, writers[0])
			}
			return
		}

		logger.lifecycle("Ambiguous command. Use '<server>: <command>' or 'all: <command>'. Use :list to view targets.")
	}

	private fun sendCommand(serverName: String, command: String, writer: BufferedWriter) {
		try {
			writer.write(command)
			writer.newLine()
			writer.flush()
			logger.lifecycle("[stdin->$serverName] $command")
		} catch (_: Exception) {
			logger.lifecycle("Failed to send command to '$serverName'.")
		}
	}

	private fun pipeToLogger(prefix: String, input: InputStream, log: (String) -> Unit) {
		Thread(
			{
				try {
					input.bufferedReader().useLines { lines ->
						lines.forEach { line ->
							formatProcessLogLine(prefix, line)?.let(log)
						}
					}
				} catch (_: Exception) {
					// Ignore stream closure during shutdown or fast process exits.
				}
			},
			"${name}-${prefix}-out"
		).apply {
			isDaemon = true
			start()
		}
	}

	private fun stopAll(processes: List<Process>) {
		processes.forEach { process ->
			val handle = process.toHandle()
			val descendants = handle.descendants().toList()
			descendants.forEach { it.destroy() }
			process.destroy()
			val timeout = if (killTimeoutSeconds.get() <= 0) 1 else killTimeoutSeconds.get()
			if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
				descendants.forEach { if (it.isAlive) it.destroyForcibly() }
				if (process.isAlive) {
					process.destroyForcibly()
				}
			}
		}
	}

	private fun decodeSpec(encoded: String): ServerProcessSpec {
		val parts = encoded.split("\u001F")
		if (parts.size < 3) {
			throw GradleException("Invalid server spec: '$encoded'")
		}
		return ServerProcessSpec(
			name = parts[0],
			workingDir = File(parts[1]),
			commandLine = parts.drop(2)
		)
	}

	companion object {
		fun encode(name: String, workingDir: File, commandLine: List<String>): String {
			return (listOf(name, workingDir.absolutePath) + commandLine).joinToString("\u001F")
		}
	}
}

private data class ServerProcessSpec(
	val name: String,
	val workingDir: File,
	val commandLine: List<String>
)
