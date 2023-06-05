plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.artifact")
}

ackpine {
	moduleName = "ktx"
}

dependencies {
	api(project(":ackpine-core"))
	api(androidx.annotation)
	api(kotlinx.coroutines.android)
}