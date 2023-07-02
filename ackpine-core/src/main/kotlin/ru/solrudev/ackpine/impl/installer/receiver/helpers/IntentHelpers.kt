package ru.solrudev.ackpine.impl.installer.receiver.helpers

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import java.io.Serializable

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getParcelableExtra(name, T::class.java)
	} else {
		getParcelableExtra(name)
	}
}

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(name: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getSerializableExtra(name, T::class.java)
	} else {
		getSerializableExtra(name) as? T
	}
}