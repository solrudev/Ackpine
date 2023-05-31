package ru.solrudev.ackpine.helpers

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry

@get:JvmSynthetic
internal val ZipEntry.isApk: Boolean
	get() = name.endsWith(".apk", ignoreCase = true) && !isDirectory

@get:JvmSynthetic
internal val File.isApk: Boolean
	get() = name.endsWith(".apk", ignoreCase = true) && !isDirectory

@JvmSynthetic
internal fun Uri.isApk(context: Context): Boolean {
	return context.contentResolver.getType(this) == "application/vnd.android.package-archive"
}