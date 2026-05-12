package me.whereareiam.spawner.platform

import org.gradle.api.GradleException

internal fun requireScenarioDownloadIdentifiers(instanceName: String, downloads: Collection<String?>) {
	if (downloads.any { it.isNullOrBlank() })
		throw GradleException("Scenario instance '$instanceName' has a download with no identifier.")
}
