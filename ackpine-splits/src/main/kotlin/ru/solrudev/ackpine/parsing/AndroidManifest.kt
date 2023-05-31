package ru.solrudev.ackpine.parsing

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

internal class AndroidManifest internal constructor(private val manifest: Map<String, String>) {

	@get:JvmSynthetic
	internal val splitName: String
		get() = manifest["split"].orEmpty()

	@get:JvmSynthetic
	internal val packageName: String
		get() = manifest.getValue("package")

	@get:JvmSynthetic
	internal val versionCode: Long
		get() = manifest.getValue("$ANDROID_NAMESPACE:versionCode").toLong()

	@get:JvmSynthetic
	internal val versionName: String
		get() = manifest["$ANDROID_NAMESPACE:versionName"].orEmpty()

	@get:JvmSynthetic
	internal val isFeatureSplit: Boolean
		get() = manifest["$ANDROID_NAMESPACE:isFeatureSplit"]?.toBooleanStrict() ?: false

	@get:JvmSynthetic
	internal val configForSplit: String
		get() = manifest["configForSplit"].orEmpty()
}