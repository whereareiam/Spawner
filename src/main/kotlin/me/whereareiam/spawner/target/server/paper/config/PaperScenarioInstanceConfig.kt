package me.whereareiam.spawner.target.server.paper.config

import me.whereareiam.spawner.config.scenario.ScenarioInstanceConfig
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class PaperScenarioInstanceConfig @Inject constructor(
	instanceName: String,
	objects: ObjectFactory
) : ScenarioInstanceConfig(instanceName, objects) {
	val downloadProvider: Property<String> = objects.property(String::class.java).convention("")
	val version: Property<String> = objects.property(String::class.java)
	val port: Property<Int> = objects.property(Int::class.java).convention(25566)
	val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)
		.convention(listOf("-Xms1G", "-Xmx1G"))
	val onlineMode: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
	val acceptEula: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
