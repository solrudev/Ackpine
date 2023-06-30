plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.library-publish")
}

ackpine {
	moduleName = "ktx"
}

dependencies {
	api(projects.ackpineCore)
	api(kotlinx.coroutines.android)
	implementation(androidx.concurrent.futures)
}