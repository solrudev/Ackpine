package ru.solrudev.ackpine

import android.net.Uri
import android.os.Build

public class ApkUriList internal constructor(baseApk: Uri) {

	private val apks = mutableListOf(baseApk)

	public fun add(apk: Uri) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			throw SplitPackagesNotSupportedException()
		}
		this.apks.add(apk)
	}

	public fun addAll(apks: Iterable<Uri>) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			throw SplitPackagesNotSupportedException()
		}
		this.apks.addAll(apks)
	}

	public fun toList(): List<Uri> {
		return apks.toList()
	}
}