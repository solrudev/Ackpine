package ru.solrudev.ackpine.installer.parameters

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Adds the specified [apk] to this [MutableApkList].
 */
@Suppress("NOTHING_TO_INLINE")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline operator fun MutableApkList.plusAssign(apk: Uri) {
	add(apk)
}

/**
 * Adds all elements of the given [apks] collection to this [MutableApkList].
 */
@Suppress("NOTHING_TO_INLINE")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline operator fun MutableApkList.plusAssign(apks: Iterable<Uri>) {
	addAll(apks)
}