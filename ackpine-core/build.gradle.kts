plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.library-publish")
	alias(libs.plugins.kotlin.ksp)
}

ackpine {
	moduleName = "core"
}

dependencies {
	ksp(androidx.room.compiler)
	api(androidx.annotation)
	api(androidx.startup)
	api(libs.listenablefuture)
	implementation(androidx.concurrent.futures)
	implementation(androidx.core.ktx)
	implementation(androidx.room.runtime)
}