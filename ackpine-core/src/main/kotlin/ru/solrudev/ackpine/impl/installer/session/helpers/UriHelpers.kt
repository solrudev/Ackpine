package ru.solrudev.ackpine.impl.installer.session.helpers

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Process
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

@JvmSynthetic
internal fun Uri.toFile(context: Context, signal: CancellationSignal? = null): File {
	if (scheme == ContentResolver.SCHEME_FILE) {
		return File(requireNotNull(path) { "Uri path is null: $this" })
	}
	try {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			context.contentResolver.openFileDescriptor(this, "r", signal)
		} else {
			context.contentResolver.openFileDescriptor(this, "r")
		}.use { fileDescriptor ->
			if (fileDescriptor == null) {
				throw NullPointerException("ParcelFileDescriptor was null: $this")
			}
			val path = "/proc/${Process.myPid()}/fd/${fileDescriptor.fd}"
			val canonicalPath = File(path).canonicalPath.replace("mnt/media_rw", "storage")
			return File(canonicalPath)
		}
	} catch (_: FileNotFoundException) {
		return File("")
	}
}

@JvmSynthetic
internal fun Context.openAssetFileDescriptor(uri: Uri, signal: CancellationSignal): AssetFileDescriptor? {
	val afd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
		contentResolver.openAssetFileDescriptor(uri, "r", signal)
	} else {
		contentResolver.openAssetFileDescriptor(uri, "r")
	}
	if (afd == null || afd.declaredLength != -1L) {
		return afd
	}
	return AssetFileDescriptor(afd.parcelFileDescriptor, afd.startOffset, contentResolver.getSize(uri))
}

private fun ContentResolver.getSize(uri: Uri): Long {
	query(uri, arrayOf(OpenableColumns.SIZE), null, null, null).use { cursor ->
		cursor ?: return -1L
		if (!cursor.moveToFirst()) {
			return -1L
		}
		return try {
			cursor.getLong(0)
		} catch (_: Throwable) {
			-1L
		}
	}
}