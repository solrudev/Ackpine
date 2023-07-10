package ru.solrudev.ackpine.impl.uninstaller.helpers

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

@JvmSynthetic
internal fun PackageManager.getApplicationLabel(packageName: String) = try {
	getApplicationInfoCompat(packageName, PackageManager.GET_META_DATA).loadLabel(this)
} catch (_: PackageManager.NameNotFoundException) {
	null
}

@Suppress("DEPRECATION")
private fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int): ApplicationInfo {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
	} else {
		getApplicationInfo(packageName, flags)
	}
}