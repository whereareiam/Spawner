package me.whereareiam.spawner.file

import java.io.File

internal fun execCommand(runDir: File, command: List<String>, waitFor: Boolean) {
	val process = ProcessBuilder(command)
		.directory(runDir)
		.inheritIO()
		.start()
	if (waitFor) {
		process.waitFor()
	}
}




