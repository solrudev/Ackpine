package ru.solrudev.ackpine.sample.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

@Suppress("DEPRECATION")
fun PackageManager.getInstalledApplicationsCompat(flags: Int): List<ApplicationInfo> {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
	} else {
		getInstalledApplications(flags)
	}
}