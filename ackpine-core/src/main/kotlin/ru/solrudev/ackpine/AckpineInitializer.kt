package ru.solrudev.ackpine

import android.content.Context
import androidx.startup.Initializer

/**
 * Initializes [Ackpine] using `androidx.startup`.
 */
public class AckpineInitializer : Initializer<Ackpine> {

	override fun create(context: Context): Ackpine {
		Ackpine.init(context)
		return Ackpine
	}

	override fun dependencies(): List<Class<out Initializer<*>>> {
		return emptyList()
	}
}