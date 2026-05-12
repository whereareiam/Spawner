package me.whereareiam.spawner.platform.target.server.paper.model

import me.whereareiam.spawner.model.FileInstallPlan
import me.whereareiam.spawner.model.InstancePlan
import me.whereareiam.spawner.model.download.DownloadPlan
import java.io.File

data class PaperInstancePlan(
	override val name: String,
	override val directory: File,
	val port: Int,
	override val version: String?,
	override val downloadProvider: String?,
	override val jvmArgs: List<String>,
	override val onlineMode: Boolean,
	val enableBungeecord: Boolean,
	val acceptEula: Boolean,
	override val rootOverlayDir: File?,
	override val installs: List<FileInstallPlan>,
	override val downloads: List<DownloadPlan>
) : InstancePlan
