package me.whereareiam.spawner

import me.whereareiam.spawner.model.FileInstallPlan
import me.whereareiam.spawner.target.TargetTasks
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

internal fun registerInstanceAliases(
	project: Project,
	displayName: String,
	prepareTaskName: String,
	runTaskName: String,
	targetTasks: TargetTasks
) {
	project.tasks.register(prepareTaskName) {
		group = "devserver"
		description = "Prepare $displayName."
		dependsOn(targetTasks.prepare)
	}
	project.tasks.register(runTaskName) {
		group = "devserver"
		description = "Run $displayName."
		dependsOn(targetTasks.run)
	}
}

internal fun installPlan(project: Project, into: String, source: Any): FileInstallPlan {
	val files = project.objects.fileCollection().from(source)
	return FileInstallPlan(files = files, into = into)
}

internal fun relativePath(baseDir: File, targetDir: File): String {
	val normalizedBase = baseDir.toPath().normalize()
	val normalizedTarget = targetDir.toPath().normalize()
	if (!normalizedTarget.startsWith(normalizedBase)) {
		throw GradleException("Configured install directory '$targetDir' must be inside '$baseDir'.")
	}
	return normalizedBase.relativize(normalizedTarget).toString().replace(File.separatorChar, '/')
}

internal fun String.toTaskSuffix(): String {
	return split(Regex("[^A-Za-z0-9]+"))
		.filter(String::isNotBlank)
		.joinToString("") { part ->
			part.replaceFirstChar { ch -> ch.uppercase() }
		}
}

internal fun String.toDisplayName(): String {
	return split(Regex("[^A-Za-z0-9]+"))
		.filter(String::isNotBlank)
		.joinToString(" ") { part ->
			part.replaceFirstChar { ch -> ch.uppercase() }
		}
}
