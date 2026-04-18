package me.whereareiam.spawner.model

import org.gradle.api.file.ConfigurableFileCollection

data class FileInstallPlan(
    val files: ConfigurableFileCollection,
    val into: String
)