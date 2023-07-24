package ru.solrudev.ackpine.installer.parameters

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * A container of APK [URIs][Uri] to install.
 */
public interface ApkList {

	/**
	 * Returns the size of the underlying list.
	 */
	public val size: Int

	/**
	 * Materializes this [ApkList] to a [List] of [URIs][Uri].
	 */
	public fun toList(): List<Uri>
}

/**
 * A container of APK [URIs][Uri] to install that supports adding elements to it.
 */
public interface MutableApkList : ApkList {

	/**
	 * Adds the specified [apk] to this [MutableApkList].
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun add(apk: Uri)

	/**
	 * Adds all elements of the given [apks] collection to this [MutableApkList].
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun addAll(apks: Iterable<Uri>)
}