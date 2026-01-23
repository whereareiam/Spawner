plugins {
	`kotlin-dsl`
	`java-gradle-plugin`
	`maven-publish`
}

group = "me.whereareiam"
version = System.getenv("VERSION") ?: "dev"

repositories {
	mavenCentral()
	gradlePluginPortal()
}

gradlePlugin {
	plugins {
		create("spawner") {
			id = "me.whereareiam.spawner"
			implementationClass = "me.whereareiam.spawner.SpawnerPlugin"
		}
	}
}

publishing {
	repositories {
		maven {
			val realm = (System.getenv("PUBLISH_REALM")
				?: if ((System.getenv("VERSION") ?: "dev").contains("dev", true)) "development" else "release")
				.lowercase()
			url = uri("https://maven.whereareiam.me/$realm")
			credentials {
				username = System.getenv("PUBLISH_USER") ?: ""
				password = System.getenv("PUBLISH_TOKEN") ?: ""
			}
		}
	}
}
