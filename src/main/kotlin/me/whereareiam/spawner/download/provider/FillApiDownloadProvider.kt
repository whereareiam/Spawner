package me.whereareiam.spawner.download.provider

import groovy.json.JsonSlurper
import me.whereareiam.spawner.download.DownloadProvider
import me.whereareiam.spawner.model.ResolvedDownload
import org.gradle.api.GradleException
import java.net.HttpURLConnection
import java.net.URI

class FillApiDownloadProvider : DownloadProvider {
	override fun resolve(project: String, version: String?, userAgent: String): ResolvedDownload {
		val (url, resolvedVersion) = resolveLatestStableDownload(project, version, userAgent)
		return ResolvedDownload(url, resolvedVersion)
	}
}

private fun resolveLatestStableDownload(project: String, version: String?, userAgent: String): Pair<String, String> {
	fun getBuilds(ver: String): List<Map<*, *>> {
		val buildsResponse = readUrl("https://fill.papermc.io/v3/projects/$project/versions/$ver/builds", userAgent)
		val parsed = JsonSlurper().parseText(buildsResponse)
		if (parsed is Map<*, *> && parsed["ok"] == false) {
			val message = parsed["message"]?.toString() ?: "Unknown error"
			throw GradleException("Failed to fetch builds for $project $ver: $message")
		}
		return (parsed as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
	}

	fun findStableUrl(ver: String): String? {
		val build = getBuilds(ver)
			.firstOrNull { it["channel"]?.toString()?.equals("STABLE", true) == true }
		return if (build != null) selectDownloadUrl(build) else null
	}

	if (!version.isNullOrBlank()) {
		val url = findStableUrl(version)
		if (url != null) return url to version
	}

	val projectResponse = readUrl("https://fill.papermc.io/v3/projects/$project", userAgent)
	val projectJson = JsonSlurper().parseText(projectResponse) as? Map<*, *> ?: emptyMap<Any, Any>()
	val versionsNode = projectJson["versions"]
	val versions = when (versionsNode) {
		is Map<*, *> -> versionsNode.values.flatMap { it as? List<*> ?: emptyList<Any>() }
		is List<*> -> versionsNode
		else -> emptyList<Any>()
	}
		.map { it.toString() }
		.distinct()
		.sortedWith(::compareVersions)

	for (ver in versions) {
		val url = findStableUrl(ver)
		if (url != null) return url to ver
	}

	throw GradleException("No stable build found for project $project")
}

private fun readUrl(url: String, userAgent: String): String {
	val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
	connection.setRequestProperty("User-Agent", userAgent)
	connection.inputStream.use { input ->
		return input.readBytes().toString(Charsets.UTF_8)
	}
}

private fun selectDownloadUrl(build: Map<*, *>): String? {
	val downloads = build["downloads"] as? Map<*, *> ?: return null
	val preferred = downloads["server:default"]
		?: downloads["application:default"]
		?: downloads.values.firstOrNull()
	val downloadMap = preferred as? Map<*, *> ?: return null
	return downloadMap["url"]?.toString()
}

private fun compareVersions(left: String, right: String): Int {
	val leftParts = left.split('.', '-', '_')
	val rightParts = right.split('.', '-', '_')
	val max = maxOf(leftParts.size, rightParts.size)
	for (index in 0 until max) {
		val leftPart = leftParts.getOrNull(index)
		val rightPart = rightParts.getOrNull(index)
		if (leftPart == null && rightPart == null) return 0
		if (leftPart == null) return 1
		if (rightPart == null) return -1
		val leftNum = leftPart.toIntOrNull()
		val rightNum = rightPart.toIntOrNull()
		val comparison = if (leftNum != null && rightNum != null) {
			rightNum.compareTo(leftNum)
		} else {
			rightPart.compareTo(leftPart)
		}
		if (comparison != 0) return comparison
	}
	return 0
}

