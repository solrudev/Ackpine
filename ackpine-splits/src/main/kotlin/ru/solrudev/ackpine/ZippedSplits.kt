package ru.solrudev.ackpine

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

public object ZippedSplits {

	@Throws(ZipException::class, IOException::class)
	@JvmStatic
	public fun getApksForFile(context: Context, file: File, filterIncompatible: Boolean): Sequence<SplitApk> {
		val zipFilePath = file.absolutePath
		return ZipFile(file).entries()
			.asSequence()
			.toSplitApkSequence(context.applicationContext, zipFilePath, filterIncompatible)
	}

	@JvmStatic
	public fun getApksForUri(context: Context, uri: Uri, filterIncompatible: Boolean): Sequence<SplitApk> {
		val file = uri.toFile(context)
		if (file.canRead()) {
			return getApksForFile(context, file, filterIncompatible)
		}
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			ZipInputStream(applicationContext.contentResolver.openInputStream(uri)).use { zipStream ->
				zipStream.entries()
					.toSplitApkSequence(applicationContext, uri.toString(), filterIncompatible)
					.forEach { yield(it) }
			}
		}
	}

	private fun Sequence<ZipEntry>.toSplitApkSequence(
		context: Context,
		zipPath: String,
		filterIncompatible: Boolean
	): Sequence<SplitApk> {
		val splitApks = mapNotNull { zipEntry ->
			SplitApk.fromZipEntry(zipPath, zipEntry)
		}
		if (!filterIncompatible) {
			return splitApks
		}
		return sequence {
			val abiSplitApks = mutableListOf<SplitApk.Libs>()
			splitApks
				.onEach { apk ->
					if (apk is SplitApk.Libs && apk.isCompatible(context)) {
						abiSplitApks += apk
					}
				}
				.filter { apk ->
					apk !is SplitApk.Libs && apk.isCompatible(context)
				}
				.forEach { yield(it) }
			abiSplitApks.sortBy { libs -> Abi.deviceAbis.indexOf(libs.abi) }
			abiSplitApks.firstOrNull()?.let { yield(it) }
		}
	}
}