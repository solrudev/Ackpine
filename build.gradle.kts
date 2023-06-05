plugins {
	id("ru.solrudev.ackpine.publishing")
}

tasks.register<Delete>("clean").configure {
	delete(rootProject.buildDir)
}