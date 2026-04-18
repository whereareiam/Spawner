package me.whereareiam.spawner.file

import me.whereareiam.spawner.model.FileInstallPlan
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal fun installFiles(baseDir: File, installs: List<FileInstallPlan>) {
	for (install in installs) {
		val relative = install.into.trim().trim('/')
		val targetDir = if (relative.isBlank()) baseDir else baseDir.resolve(relative)
		targetDir.mkdirs()
		for (file in install.files.files) {
			copyPath(file, targetDir.resolve(file.name))
		}
	}
}

internal fun installOverlay(sourceDir: File, targetDir: File) {
	if (!sourceDir.exists()) return
	copyDirectoryContents(sourceDir, targetDir)
}

internal fun installFile(baseDir: File, into: String, source: File, targetName: String = source.name) {
	val relative = into.trim().trim('/')
	val targetDir = if (relative.isBlank()) baseDir else baseDir.resolve(relative)
	targetDir.mkdirs()
	copyPath(source, targetDir.resolve(targetName))
}

private fun copyDirectoryContents(sourceDir: File, targetDir: File) {
	sourceDir.walkTopDown().forEach { source ->
		if (source == sourceDir) return@forEach
		val relative = source.relativeTo(sourceDir)
		val target = targetDir.resolve(relative.path)
		if (source.isDirectory) {
			target.mkdirs()
			return@forEach
		}
		copyPath(source, target)
	}
}

private fun copyPath(source: File, target: File) {
	if (source.isDirectory) {
		copyDirectoryContents(source, target)
		return
	}
	target.parentFile?.mkdirs()
	Files.copy(
		source.toPath(),
		target.toPath(),
		StandardCopyOption.REPLACE_EXISTING
	)
}
