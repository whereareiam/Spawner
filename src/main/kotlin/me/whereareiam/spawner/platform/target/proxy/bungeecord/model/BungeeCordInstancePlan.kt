package me.whereareiam.spawner.platform.target.proxy.bungeecord.model

import me.whereareiam.spawner.model.FileInstallPlan
import me.whereareiam.spawner.model.InstancePlan
import me.whereareiam.spawner.model.download.DownloadPlan
import java.io.File

data class BungeeCordInstancePlan(
	override val name: String,
	override val directory: File,
	val port: Int,
	override val version: String?,
	override val downloadProvider: String?,
	override val jvmArgs: List<String>,
	override val onlineMode: Boolean,
	val ipForward: Boolean,
	val servers: List<BungeeCordServerPlan>,
	val tryServers: List<String>,
	override val rootOverlayDir: File?,
	override val installs: List<FileInstallPlan>,
	override val downloads: List<DownloadPlan>
) : InstancePlan
