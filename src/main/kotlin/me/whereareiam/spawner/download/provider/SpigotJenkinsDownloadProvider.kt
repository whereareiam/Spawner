package me.whereareiam.spawner.download.provider

import groovy.json.JsonSlurper
import me.whereareiam.spawner.model.download.DownloadPlan
import me.whereareiam.spawner.model.download.ResolvedDownload
import org.gradle.api.GradleException
import java.net.HttpURLConnection
import java.net.URI

class SpigotJenkinsDownloadProvider : DownloadProvider {
	override fun resolve(download: DownloadPlan, userAgent: String): ResolvedDownload {
		val jobName = download.identifier.ifBlank {
			throw GradleException("Spigot Jenkins download identifier must not be blank.")
		}
		val buildRef = download.version?.takeIf { it.isNotBlank() } ?: "lastSuccessfulBuild"
		val buildNode = readJson(
			"https://hub.spigotmc.org/jenkins/job/$jobName/$buildRef/api/json",
			userAgent
		) as? Map<*, *> ?: throw GradleException("Unexpected Jenkins response for job '$jobName'")
		val buildNumber = buildNode["number"]?.toString()
			?: throw GradleException("Jenkins build number missing for job '$jobName'")
		val artifacts = buildNode["artifacts"] as? List<*>
			?: throw GradleException("Jenkins artifacts missing for job '$jobName'")
		val requestedName = download.fileName?.takeIf { it.isNotBlank() }
		val artifact = artifacts
			.filterIsInstance<Map<*, *>>()
			.firstOrNull { entry ->
				val fileName = entry["fileName"]?.toString()
				requestedName == null || fileName.equals(requestedName, true)
			}
			?: throw GradleException(
				"No Jenkins artifact found for job '$jobName'" +
					(if (requestedName != null) " with file name '$requestedName'" else "")
			)
		val relativePath = artifact["relativePath"]?.toString()
			?: throw GradleException("Artifact path missing for job '$jobName'")
		val fileName = artifact["fileName"]?.toString()
			?: requestedName
			?: throw GradleException("Artifact file name missing for job '$jobName'")
		return ResolvedDownload(
			url = "https://hub.spigotmc.org/jenkins/job/$jobName/$buildRef/artifact/$relativePath",
			version = buildNumber,
			fileName = fileName
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
