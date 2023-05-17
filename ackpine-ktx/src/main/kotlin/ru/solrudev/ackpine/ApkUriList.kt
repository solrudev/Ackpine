package ru.solrudev.ackpine

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

@Suppress("NOTHING_TO_INLINE")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline operator fun ApkUriList.plusAssign(apk: Uri) {
	add(apk)
}

@Suppress("NOTHING_TO_INLINE")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline operator fun ApkUriList.plusAssign(apks: Iterable<Uri>) {
	addAll(apks)
}