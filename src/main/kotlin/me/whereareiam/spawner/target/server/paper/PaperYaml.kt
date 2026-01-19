package me.whereareiam.spawner.target.server.paper

import java.io.File

internal fun updateSpigotBungeecordSetting(file: File, enabled: Boolean) {
	val lines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
	val valueLine = "  bungeecord: $enabled"
	val settingsIndex = lines.indexOfFirst { it.trim() == "settings:" }
	if (settingsIndex == -1) {
		lines.add("settings:")
		lines.add(valueLine)
	} else {
		var index = settingsIndex + 1
		var updated = false
		while (index < lines.size) {
			val line = lines[index]
			val trimmed = line.trim()
			if (trimmed.isNotEmpty() && !line.startsWith("  ")) {
				break
			}
			if (trimmed.startsWith("bungeecord:")) {
				lines[index] = valueLine
				updated = true
				break
			}
			index++
		}
		if (!updated) {
			lines.add(index, valueLine)
		}
	}
	file.parentFile.mkdirs()
	file.writeText(lines.joinToString(System.lineSeparator()) + System.lineSeparator())
}
