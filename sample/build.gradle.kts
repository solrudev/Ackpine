import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val androidGradleVersion: String by rootProject.extra
val packageName = "ru.solrudev.ackpine.sample"

plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("kapt")
}

kotlin {
	jvmToolchain(17)
}

android {
	compileSdk = 33
	buildToolsVersion = "33.0.2"
	namespace = packageName

	defaultConfig {
		applicationId = packageName
		minSdk = 16
		targetSdk = 33
		versionCode = 1
		versionName = "1.0"
	}

	buildTypes {
		named("release") {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		dataBinding = true
		viewBinding = true
	}
}

tasks.withType<KotlinJvmCompile>().configureEach {
	compilerOptions {
		freeCompilerArgs.add("-Xjvm-default=all")
	}
}

dependencies {
	kapt("androidx.databinding:databinding-compiler:$androidGradleVersion")
	implementation("androidx.activity:activity-ktx:1.7.1")
	implementation("com.google.android.material:material:1.9.0")
	implementation(project(":ackpine-ktx"))
	implementation(project(":ackpine-coroutines"))
}