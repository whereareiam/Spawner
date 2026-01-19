package me.whereareiam.spawner.file

import java.io.File
import kotlin.collections.iterator

internal fun updatePropertyFile(file: File, entries: Map<String, String>) {
	val lines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
	for ((key, value) in entries) {
		val index = lines.indexOfFirst { it.startsWith("$key=") }
		val line = "$key=$value"
		if (index >= 0) {
			lines[index] = line
		} else {
			lines.add(line)
		}
	}
	file.parentFile.mkdirs()
	file.writeText(lines.joinToString(System.lineSeparator()) + System.lineSeparator())
}




