package me.whereareiam.spawner.platform.target.proxy.velocity.config

import me.whereareiam.spawner.config.scenario.ScenarioInstanceConfig
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class VelocityScenarioInstanceConfig @Inject constructor(
	instanceName: String,
	private val objects: ObjectFactory
) : ScenarioInstanceConfig(instanceName, objects) {
	val downloadProvider: Property<String> = objects.property(String::class.java).convention("")
	val version: Property<String> = objects.property(String::class.java)
	val port: Property<Int> = objects.property(Int::class.java).convention(25565)
	val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)
		.convention(listOf("-Xms1G", "-Xmx1G"))
	val onlineMode: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
	val forwardingMode: Property<String> = objects.property(String::class.java).convention("none")
	val tryServers: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())

	private val servers = linkedMapOf<String, VelocityServerEntryConfig>()

	fun server(name: String, address: String) {
		val entry = servers.getOrPut(name) {
			objects.newInstance(VelocityServerEntryConfig::class.java, name)
		}
		entry.address(address)
	}

	fun server(name: String, action: Action<in VelocityServerEntryConfig>) {
		val entry = servers.getOrPut(name) {
			objects.newInstance(VelocityServerEntryConfig::class.java, name)
		}
		action.execute(entry)
	}

	fun tryServers(vararg names: String) {
		tryServers.set(names.toList())
	}

	fun servers(): Collection<VelocityServerEntryConfig> = servers.values
}
