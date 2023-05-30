package ru.solrudev.ackpine.helpers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.Process
import android.provider.OpenableColumns
import java.io.File

@JvmSynthetic
internal fun String.toUri(): Uri = Uri.parse(this)

@JvmSynthetic
internal fun Uri.toFile(context: Context, signal: CancellationSignal? = null): File {
	if (scheme == ContentResolver.SCHEME_FILE) {
		return File(requireNotNull(path) { "Uri path is null: $this" })
	}
	context.contentResolver.openFileDescriptor(this, "r", signal).use { fileDescriptor ->
		if (fileDescriptor == null) {
			throw NullPointerException("ParcelFileDescriptor was null: $this")
		}
		val path = "/proc/${Process.myPid()}/fd/${fileDescriptor.fd}"
		val canonicalPath = File(path).canonicalPath.replace("mnt/media_rw", "storage")
		return File(canonicalPath)
	}
}

@JvmSynthetic
internal fun Uri.name(context: Context): String? {
	context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
		if (cursor == null) {
			return null
		}
		if (!cursor.moveToFirst()) {
			return null
		}
		return cursor.getString(0)
	}
}