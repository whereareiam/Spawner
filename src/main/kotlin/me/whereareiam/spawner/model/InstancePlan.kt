package me.whereareiam.spawner.model

import me.whereareiam.spawner.model.download.DownloadPlan
import java.io.File

interface InstancePlan {
	val name: String
	val directory: File
	val version: String?
	val downloadProvider: String?
	val jvmArgs: List<String>
	val onlineMode: Boolean
	val rootOverlayDir: File?
	val installs: List<FileInstallPlan>
	val downloads: List<DownloadPlan>
}