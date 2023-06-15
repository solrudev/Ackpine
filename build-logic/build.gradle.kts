plugins {
	`kotlin-dsl`
}

kotlin {
	jvmToolchain(17)
}

gradlePlugin {
	plugins {
		register("ackpine") {
			id = "ru.solrudev.ackpine.library"
			implementationClass = "ru.solrudev.ackpine.gradle.AckpineLibraryPlugin"
		}
		register("ackpine-artifact-publish") {
			id = "ru.solrudev.ackpine.artifact-publish"
			implementationClass = "ru.solrudev.ackpine.gradle.publishing.AckpineArtifactPublishPlugin"
		}
		register("ackpine-publishing") {
			id = "ru.solrudev.ackpine.publishing"
			implementationClass = "ru.solrudev.ackpine.gradle.publishing.AckpinePublishingPlugin"
		}
	}
}

dependencies {
	implementation(libs.plugin.agp)
	implementation(libs.plugin.kotlin.android)
	implementation(libs.plugin.nexus.publish)
}