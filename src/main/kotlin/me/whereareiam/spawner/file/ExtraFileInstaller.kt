package me.whereareiam.spawner.file

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal fun installExtraFiles(targetDir: File, files: Set<File>) {
	if (files.isEmpty()) return
	targetDir.mkdirs()
	for (file in files) {
		if (!file.exists()) continue
		Files.copy(
			file.toPath(),
			targetDir.resolve(file.name).toPath(),
			StandardCopyOption.REPLACE_EXISTING
		)
	}
}
