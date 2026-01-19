package me.whereareiam.spawner.download

import java.io.File
import java.net.HttpURLConnection
import java.net.URI

internal fun downloadFile(url: String, target: File, userAgent: String) {
	val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
	connection.setRequestProperty("User-Agent", userAgent)
	connection.inputStream.use { input ->
		target.outputStream().use { output -> input.copyTo(output) }
	}
}

