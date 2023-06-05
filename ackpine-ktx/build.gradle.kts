plugins {
	id("ru.solrudev.ackpine.library")
}

ackpine {
	moduleName = "ktx"
}

dependencies {
	api(project(":ackpine-core"))
	api(androidx.annotation)
}