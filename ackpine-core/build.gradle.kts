import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val publishGroupId: String by rootProject.extra
val publishVersion: String by rootProject.extra
val publishArtifactId = "ackpine-core"

plugins {
	id("com.android.library")
	kotlin("android")
	`maven-publish`
	signing
}

kotlin {
	jvmToolchain(17)
	sourceSets.configureEach {
		languageSettings {
			enableLanguageFeature("DataObjects")
		}
	}
}

android {
	compileSdk = 33
	buildToolsVersion = "33.0.2"
	namespace = "ru.solrudev.ackpine"

	defaultConfig {
		minSdk = 16
		version = publishVersion
		consumerProguardFiles("consumer-rules.pro")
	}

	publishing {
		singleVariant("release") {
			withSourcesJar()
		}
	}

	buildTypes {
		named("release") {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
}

tasks.withType<KotlinJvmCompile>().configureEach {
	compilerOptions {
		jvmTarget.set(JVM_1_8)
		freeCompilerArgs.addAll("-Xexplicit-api=strict", "-Xjvm-default=all")
	}
}

dependencies {
	api("androidx.startup:startup-runtime:1.1.1")
	implementation("androidx.appcompat:appcompat:1.6.1")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
}

signing {
	val keyId = rootProject.extra["signing.keyId"] as String
	val key = rootProject.extra["signing.key"] as String
	val password = rootProject.extra["signing.password"] as String
	useInMemoryPgpKeys(keyId, key, password)
	sign(publishing.publications)
}