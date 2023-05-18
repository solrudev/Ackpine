package ru.solrudev.ackpine

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

public interface ApkList {
	public val size: Int
	public fun toList(): List<Uri>
}

public interface MutableApkList : ApkList {

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun add(apk: Uri)

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun addAll(apks: Iterable<Uri>)
}