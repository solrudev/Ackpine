package ru.solrudev.ackpine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Process
import java.io.File

@JvmSynthetic
internal fun String.toUri(): Uri = Uri.parse(this)

@JvmSynthetic
internal fun Uri.toFile(context: Context): File {
	if (scheme == ContentResolver.SCHEME_FILE) {
		return File(requireNotNull(path) { "Uri path is null: $this" })
	}
	context.contentResolver.openFileDescriptor(this, "r").use { fileDescriptor ->
		if (fileDescriptor == null) {
			throw NullPointerException("ParcelFileDescriptor from $this was null")
		}
		val path = "/proc/${Process.myPid()}/fd/${fileDescriptor.fd}"
		val canonicalPath = File(path).canonicalPath.replace("mnt/media_rw", "storage")
		return File(canonicalPath)
	}
}