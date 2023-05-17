rootProject.name = "Ackpine"
include(":ackpine-core")
include(":ackpine-ktx")
include(":ackpine-coroutines")
include(":ackpine-splitszip")
include(":sample")

pluginManagement {
	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
	}
}
