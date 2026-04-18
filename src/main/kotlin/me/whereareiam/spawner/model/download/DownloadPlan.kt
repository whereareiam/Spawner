package me.whereareiam.spawner.model.download

data class DownloadPlan(
	val provider: String?,
	val identifier: String,
	val version: String?,
	val versionType: String,
	val fileName: String?,
	val into: String = ""
)
