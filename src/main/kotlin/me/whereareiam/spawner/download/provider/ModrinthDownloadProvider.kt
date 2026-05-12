package me.whereareiam.spawner.download.provider

import groovy.json.JsonSlurper
import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.model.download.ResolvedDownload
import org.gradle.api.GradleException
import java.net.HttpURLConnection
import java.net.URI

class ModrinthDownloadProvider : DownloadProvider {
	override fun resolve(download: DownloadPlan, userAgent: String): ResolvedDownload {
		val projectId = download.identifier.ifBlank {
			throw GradleException("Modrinth download identifier must not be blank.")
		}
		val versions = readJson("https://api.modrinth.com/v2/project/$projectId/version", userAgent) as? List<*>
			?: throw GradleException("Unexpected Modrinth response for project '$projectId'")

		val requestedVersion = download.version?.takeIf { it.isNotBlank() }
		val requestedType = download.versionType.ifBlank { "release" }

		val release = versions
			.filterIsInstance<Map<*, *>>()
			.firstOrNull { version ->
				val matchesVersion = requestedVersion == null || version["version_number"]?.toString() == requestedVersion
				val matchesType = version["version_type"]?.toString()?.equals(requestedType, true) == true
				val listed = version["status"]?.toString()?.equals("listed", true) == true
				matchesVersion && matchesType && listed
			}
			?: throw GradleException(
				"No listed Modrinth version found for project '$projectId' with versionType '$requestedType'" +
					(if (requestedVersion != null) " and version '$requestedVersion'" else "")
			)

		val file = (release["files"] as? List<*>)
			?.filterIsInstance<Map<*, *>>()
			?.firstOrNull { it["primary"] == true }
			?: throw GradleException("No primary file found for Modrinth project '$projectId'")

		val url = file["url"]?.toString()
			?: throw GradleException("Primary file URL missing for Modrinth project '$projectId'")
		val fileName = download.fileName?.takeIf { it.isNotBlank() }
			?: file["filename"]?.toString()
			?: throw GradleException("Primary file name missing for Modrinth project '$projectId'")
		val version = release["version_number"]?.toString() ?: requestedVersion ?: "unknown"

		return ResolvedDownload(
			url = url,
			fileName = fileName,
			version = version
		)
	}
}

private fun readJson(url: String, userAgent: String): Any {
	val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
	connection.setRequestProperty("User-Agent", userAgent)
	connection.inputStream.use { input ->
		return JsonSlurper().parseText(input.readBytes().toString(Charsets.UTF_8))
	}
}
