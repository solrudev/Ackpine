rootProject.name = "Ackpine"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
	includeBuild("build-logic")

	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
	}

	versionCatalogs {
		register("androidx") {
			from(files("gradle/androidx.versions.toml"))
		}
		register("kotlinx") {
			from(files("gradle/kotlinx.versions.toml"))
		}
	}
}

include(":ackpine-core")
include(":ackpine-ktx")
include(":ackpine-splits")
include(":ackpine-assets")
include(":sample")