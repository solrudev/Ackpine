plugins {
	id("ru.solrudev.ackpine.library")
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