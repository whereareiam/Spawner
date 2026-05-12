package me.whereareiam.spawner.target.proxy.bungeecord.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class BungeeCordServerEntryConfig @Inject constructor(
	private val serverName: String,
	objects: ObjectFactory
) {
	val address: Property<String> = objects.property(String::class.java)

	fun getName(): String = serverName

	fun address(value: String) {
		address.set(value)
	}
}
