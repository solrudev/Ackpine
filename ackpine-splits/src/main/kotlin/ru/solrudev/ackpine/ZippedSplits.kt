package ru.solrudev.ackpine

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

public object ZippedSplits {

	@JvmStatic
	public fun getApksForFile(context: Context, file: File, filterIncompatible: Boolean): Sequence<SplitApk> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			ZipFile(file).use { zipFile ->
				zipFile.entries()
					.asSequence()
					.toSplitApkSequence(applicationContext, file.absolutePath, filterIncompatible)
					.forEach { yield(it) }
			}
		}
	}

	@JvmStatic
	public fun getApksForUri(context: Context, uri: Uri, filterIncompatible: Boolean): Sequence<SplitApk> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			val file = uri.toFile(applicationContext)
			if (file.canRead()) {
				yieldAll(getApksForFile(applicationContext, file, filterIncompatible))
				return@sequence
			}
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