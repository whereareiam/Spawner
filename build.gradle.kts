plugins {
	`kotlin-dsl`
	`java-gradle-plugin`
	id("com.gradle.plugin-publish") version "2.0.0"
}

group = "me.whereareiam"
version = System.getenv("VERSION") ?: "dev"

repositories {
	mavenCentral()
	gradlePluginPortal()
}

gradlePlugin {
	website.set("https://github.com/whereareiam/spawner")
	vcsUrl.set("https://github.com/whereareiam/spawner")
	plugins {
		create("spawner") {
			id = "me.whereareiam.spawner"
			implementationClass = "me.whereareiam.spawner.SpawnerPlugin"
			displayName = "Spawner"
			description = "Spawn and manage local dev servers for different platforms"
			tags.set(listOf("minecraft", "paper", "velocity", "devserver"))
		}
	}
}
