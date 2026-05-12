package me.whereareiam.spawner.config.scenario

import me.whereareiam.spawner.platform.target.proxy.bungeecord.config.BungeeCordScenarioInstanceConfig
import me.whereareiam.spawner.platform.target.proxy.velocity.config.VelocityScenarioInstanceConfig
import me.whereareiam.spawner.platform.target.server.paper.config.PaperScenarioInstanceConfig
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class SpawnerScenariosConfig @Inject constructor(private val objects: ObjectFactory) {
	private val scenarios = linkedMapOf<String, SpawnerScenarioConfig>()

	fun register(name: String, action: Action<in SpawnerScenarioConfig>) {
		action.execute(maybeCreate(name))
	}

	fun maybeCreate(name: String): SpawnerScenarioConfig {
		return scenarios.getOrPut(name) {
			objects.newInstance(SpawnerScenarioConfig::class.java, name)
		}
	}

	fun isEmpty(): Boolean = scenarios.isEmpty()

	fun all(): Collection<SpawnerScenarioConfig> = scenarios.values
}

open class SpawnerScenarioConfig @Inject constructor(
	private val scenarioName: String,
	private val objects: ObjectFactory
) {
	private val paperInstances = linkedMapOf<String, PaperScenarioInstanceConfig>()
	private val bungeecordInstances = linkedMapOf<String, BungeeCordScenarioInstanceConfig>()
	private val velocityInstances = linkedMapOf<String, VelocityScenarioInstanceConfig>()

	fun getName(): String = scenarioName

	fun paper(name: String, action: Action<in PaperScenarioInstanceConfig>) {
		action.execute(paper(name))
	}

	fun velocity(name: String, action: Action<in VelocityScenarioInstanceConfig>) {
		action.execute(velocity(name))
	}

	fun bungeecord(name: String, action: Action<in BungeeCordScenarioInstanceConfig>) {
		action.execute(bungeecord(name))
	}

	fun paper(name: String): PaperScenarioInstanceConfig {
		return paperInstances.getOrPut(name) {
			objects.newInstance(PaperScenarioInstanceConfig::class.java, name)
		}
	}

	fun bungeecord(name: String): BungeeCordScenarioInstanceConfig {
		return bungeecordInstances.getOrPut(name) {
			objects.newInstance(BungeeCordScenarioInstanceConfig::class.java, name)
		}
	}

	fun velocity(name: String): VelocityScenarioInstanceConfig {
		return velocityInstances.getOrPut(name) {
			objects.newInstance(VelocityScenarioInstanceConfig::class.java, name)
		}
	}

	fun papers(): Collection<PaperScenarioInstanceConfig> = paperInstances.values

	fun bungeecords(): Collection<BungeeCordScenarioInstanceConfig> = bungeecordInstances.values

	fun velocities(): Collection<VelocityScenarioInstanceConfig> = velocityInstances.values

	fun proxies(): Collection<ScenarioInstanceConfig> = bungeecordInstances.values + velocityInstances.values
}
