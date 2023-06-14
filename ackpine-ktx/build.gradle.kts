plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.artifact")
}

ackpine {
	moduleName = "ktx"
}

dependencies {
	api(projects.ackpineCore)
	api(androidx.annotation)
	api(kotlinx.coroutines.android)
}