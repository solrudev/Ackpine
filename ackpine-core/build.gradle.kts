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
	api(androidx.startup.runtime)
	api(androidx.annotation)
	api(libs.listenablefuture)
	implementation(androidx.concurrent.futures)
	implementation(androidx.appcompat)
	implementation(androidx.lifecycle.runtime.ktx)
	implementation(androidx.room.ktx)
}