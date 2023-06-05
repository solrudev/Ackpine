plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.artifact")
}

ackpine {
	moduleName = "core"
}

dependencies {
	api(androidx.startup.runtime)
	api(androidx.annotation)
	implementation(androidx.appcompat)
	implementation(androidx.lifecycle.runtime.ktx)
}