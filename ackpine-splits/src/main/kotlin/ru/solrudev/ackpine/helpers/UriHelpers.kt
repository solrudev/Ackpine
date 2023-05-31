package ru.solrudev.ackpine.helpers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.Process
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

@JvmSynthetic
internal fun String.toUri(): Uri = Uri.parse(this)

@JvmSynthetic
internal fun Uri.toFile(context: Context, signal: CancellationSignal? = null): File {
	if (scheme == ContentResolver.SCHEME_FILE) {
		return File(requireNotNull(path) { "Uri path is null: $this" })
	}
	try {
		context.contentResolver.openFileDescriptor(this, "r", signal).use { fileDescriptor ->
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
internal fun Uri.displayNameAndSize(context: Context): Pair<String, Long> {
	context.contentResolver.query(
		this,
		arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
		null, null, null
	).use { cursor ->
		cursor ?: return "" to -1L
		if (!cursor.moveToFirst()) {
			return "" to -1L
		}
		return cursor.getString(0).orEmpty() to cursor.getLong(1)
	}
}