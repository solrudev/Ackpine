val packageName = "ru.solrudev.ackpine.sample"

plugins {
	id(libs.plugins.android.application.get().pluginId)
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

android {
	compileSdk = 33
	buildToolsVersion = "33.0.2"
	namespace = packageName

	defaultConfig {
		applicationId = packageName
		minSdk = 21
		targetSdk = 33
		versionCode = 1
		versionName = "1.0"
	}

	buildTypes {
		named("release") {
			isMinifyEnabled = true
			isShrinkResources = true
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

dependencies {
	implementation(projects.ackpineSplits)
	implementation(androidx.activity.java)
	implementation(androidx.appcompat)
	implementation(androidx.recyclerview)
	implementation(androidx.constraintlayout)
	implementation(androidx.bundles.lifecycle.java)
	implementation(androidx.bundles.navigation.java)
	implementation(libs.materialcomponents)
	implementation(libs.guava)
}