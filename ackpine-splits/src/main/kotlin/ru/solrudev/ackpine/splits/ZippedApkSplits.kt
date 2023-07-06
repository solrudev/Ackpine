package ru.solrudev.ackpine.splits

import android.content.Context
import android.net.Uri
import ru.solrudev.ackpine.helpers.entries
import ru.solrudev.ackpine.helpers.toFile
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Factories for [APK splits][Apk] [sequences][Sequence].
 */
public object ZippedApkSplits {

	/**
	 * Returns a lazy sequence of [APK splits][Apk] contained within zipped [file] (such as APKS, APKM, XAPK, ZIP).
	 *
	 * **Attention:** iteration of this sequence is blocking due to I/O operations. Make sure you move it off to another
	 * thread.
	 *
	 * The returned sequence is constrained to be iterated only once.
	 */
	@JvmStatic
	public fun getApksForFile(file: File): Sequence<Apk> = sequence {
		ZipFile(file).use { zipFile ->
			zipFile.entries()
				.asSequence()
				.mapNotNull { zipEntry ->
					zipFile.getInputStream(zipEntry).use { entryStream ->
						Apk.fromZipEntry(file.absolutePath, zipEntry, entryStream)
					}
				}
				.forEach { yield(it) }
		}
	}

	/**
	 * Returns a lazy sequence of [APK splits][Apk] contained within zipped file (such as APKS, APKM, XAPK, ZIP) at provided
	 * [uri].
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 *
	 * **Attention:** iteration of this sequence is blocking due to I/O operations. Make sure you move it off to another
	 * thread.
	 *
	 * The returned sequence is constrained to be iterated only once.
	 */
	@JvmStatic
	public fun getApksForUri(context: Context, uri: Uri): Sequence<Apk> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			val file = uri.toFile(applicationContext)
			if (file.canRead()) {
				yieldAll(getApksForFile(file))
				return@sequence
			}
			// fall back to ZipInputStream if file is not readable directly
			ZipInputStream(applicationContext.contentResolver.openInputStream(uri)).use { zipStream ->
				zipStream.entries()
					.mapNotNull { zipEntry -> Apk.fromZipEntry(uri.toString(), zipEntry, zipStream) }
					.forEach { yield(it) }
			}
		}
	}
}