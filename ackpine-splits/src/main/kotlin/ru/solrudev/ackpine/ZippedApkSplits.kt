package ru.solrudev.ackpine

import android.content.Context
import android.net.Uri
import ru.solrudev.ackpine.helpers.entries
import ru.solrudev.ackpine.helpers.toFile
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

public object ZippedApkSplits {

	@JvmStatic
	public fun getApksForFile(file: File): Sequence<ApkSplit> = sequence {
		ZipFile(file).use { zipFile ->
			zipFile.entries()
				.asSequence()
				.mapNotNull { zipEntry -> ApkSplit.fromZipEntry(file.absolutePath, zipEntry) }
				.forEach { yield(it) }
		}
	}

	@JvmStatic
	public fun getApksForUri(context: Context, uri: Uri): Sequence<ApkSplit> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			val file = uri.toFile(applicationContext)
			if (file.canRead()) {
				yieldAll(getApksForFile(file))
				return@sequence
			}
			ZipInputStream(applicationContext.contentResolver.openInputStream(uri)).use { zipStream ->
				zipStream.entries()
					.mapNotNull { zipEntry -> ApkSplit.fromZipEntry(uri.toString(), zipEntry) }
					.forEach { yield(it) }
			}
		}
	}
}