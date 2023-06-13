plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.artifact")
	alias(libs.plugins.kotlin.ksp)
}

ackpine {
	moduleName = "core"
}

dependencies {
	ksp(androidx.room.compiler)
	api(androidx.startup.runtime)
	api(androidx.annotation)
	implementation(androidx.appcompat)
	implementation(androidx.lifecycle.runtime.ktx)
	implementation(androidx.room.ktx)
}