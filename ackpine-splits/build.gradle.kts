plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.library-publish")
}

ackpine {
	moduleName = "splits"
}

android {
	defaultConfig {
		minSdk = 21
	}
}

dependencies {
	implementation(androidx.core)
	implementation(libs.apksig)
}