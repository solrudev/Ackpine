import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val packageName = "ru.solrudev.ackpine.sample"

plugins {
	id(libs.plugins.android.application.get().pluginId)
	id(libs.plugins.kotlin.android.get().pluginId)
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
		viewBinding = true
	}
}

tasks.withType<KotlinJvmCompile>().configureEach {
	compilerOptions {
		freeCompilerArgs.add("-Xjvm-default=all")
	}
}

dependencies {
	implementation(projects.ackpineKtx)
	implementation(androidx.activity.ktx)
	implementation(libs.materialcomponents)
}