import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val publishGroupId: String by rootProject.extra
val publishVersion: String by rootProject.extra
val publishArtifactId = "ackpine-coroutines"

plugins {
	id("com.android.library")
	kotlin("android")
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
		consumerProguardFiles("consumer-rules.pro")
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

tasks.withType<KotlinJvmCompile>().configureEach {
	compilerOptions {
		jvmTarget.set(JVM_1_8)
		freeCompilerArgs.addAll("-Xexplicit-api=strict", "-Xjvm-default=all")
	}
}

dependencies {
	api(project(":ackpine-core"))
	api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
}