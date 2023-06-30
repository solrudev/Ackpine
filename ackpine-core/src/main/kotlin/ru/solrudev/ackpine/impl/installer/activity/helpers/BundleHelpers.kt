package ru.solrudev.ackpine.impl.installer.activity.helpers

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Parcelable> Bundle.getParcelableCompat(name: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getParcelable(name, T::class.java)
	} else {
		getParcelable(name)
	}
}

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Serializable> Bundle.getSerializableCompat(name: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getSerializable(name, T::class.java)
	} else {
		getSerializable(name) as? T
	}
}