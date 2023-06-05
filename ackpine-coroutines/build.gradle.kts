plugins {
	id("ru.solrudev.ackpine.library")
}

ackpine {
	moduleName = "coroutines"
}

dependencies {
	api(project(":ackpine-core"))
	api(kotlinx.coroutines.android)
}