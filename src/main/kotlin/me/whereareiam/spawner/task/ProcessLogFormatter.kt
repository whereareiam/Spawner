package me.whereareiam.spawner.task

private val ansiEscapeRegex = Regex("""\u001B\[[0-9;?]*[ -/]*[@-~]""")

internal fun formatProcessLogLine(prefix: String, line: String): String? {
	val normalized = normalizeProcessLogLine(line)
	if (normalized.isEmpty()) return null
	if (normalized.all { it == '>' }) return null
	if (prefix.isEmpty()) return line
	return "[$prefix] $line"
}

internal fun normalizeProcessLogLine(line: String): String {
	return ansiEscapeRegex
		.replace(line, "")
		.replace("\r", "")
		.trim()
}
