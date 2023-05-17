import java.util.*

val publishGroupId by extra("ru.solrudev.ackpine")
val publishVersion by extra("0.0.1")
group = publishGroupId
version = publishVersion

plugins {
	id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
	id("com.android.library") version "8.0.1" apply false
	id("org.jetbrains.kotlin.android") version "1.8.20" apply false
}

buildscript {
	val androidGradleVersion by extra("8.0.1")
	repositories {
		google()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:$androidGradleVersion")
		classpath(kotlin("gradle-plugin", "1.8.20"))
	}
}

tasks.register<Delete>("clean").configure {
	delete(rootProject.buildDir)
}

val ossrhUsername by extra("")
val ossrhPassword by extra("")
val sonatypeStagingProfileId by extra("")
extra["signing.keyId"] = ""
extra["signing.password"] = ""
extra["signing.key"] = ""

val secretPropertiesFile = project.rootProject.file("local.properties")
if (secretPropertiesFile.exists()) {
	Properties().run {
		secretPropertiesFile.inputStream().use(::load)
		forEach { name, value -> extra[name as String] = value }
	}
}

extra["ossrhUsername"] = System.getenv("OSSRH_USERNAME") ?: extra["ossrhUsername"]
extra["ossrhPassword"] = System.getenv("OSSRH_PASSWORD") ?: extra["ossrhPassword"]
extra["sonatypeStagingProfileId"] = System.getenv("SONATYPE_STAGING_PROFILE_ID") ?: extra["sonatypeStagingProfileId"]
extra["signing.keyId"] = System.getenv("SIGNING_KEY_ID") ?: extra["signing.keyId"]
extra["signing.password"] = System.getenv("SIGNING_PASSWORD") ?: extra["signing.password"]
extra["signing.key"] = System.getenv("SIGNING_KEY") ?: extra["signing.key"]

nexusPublishing {
	this.repositories {
		sonatype {
			stagingProfileId.set(sonatypeStagingProfileId)
			username.set(ossrhUsername)
			password.set(ossrhPassword)
			nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
			snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
		}
	}
}