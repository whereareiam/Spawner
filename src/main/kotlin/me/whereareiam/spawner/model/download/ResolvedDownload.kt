package me.whereareiam.spawner.model.download

data class ResolvedDownload(
	val url: String,
	val version: String,
	val fileName: String? = null
)
