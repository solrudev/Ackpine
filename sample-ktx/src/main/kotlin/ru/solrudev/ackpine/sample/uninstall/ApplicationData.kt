package ru.solrudev.ackpine.sample.uninstall

import android.graphics.drawable.Drawable

data class ApplicationData(
	val name: String,
	val packageName: String,
	val icon: Drawable
)