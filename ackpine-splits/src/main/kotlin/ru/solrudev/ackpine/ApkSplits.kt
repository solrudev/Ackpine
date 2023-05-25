package ru.solrudev.ackpine

import android.content.Context

public object ApkSplits {

	@JvmStatic
	public fun Sequence<ApkSplit>.filterIncompatible(context: Context): Sequence<ApkSplit> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			val libsApks = mutableListOf<ApkSplit.Libs>()
			this@filterIncompatible
				.onEach { apk ->
					if (apk is ApkSplit.Libs && apk.isCompatible(applicationContext)) {
						libsApks += apk
					}
				}
				.filter { apk -> apk !is ApkSplit.Libs && apk.isCompatible(applicationContext) }
				.forEach { yield(it) }
			libsApks.sortBy { apk -> Abi.deviceAbis.indexOf(apk.abi) }
			libsApks.firstOrNull()?.let { yield(it) }
		}
	}

	@JvmStatic
	public fun Iterable<ApkSplit>.filterIncompatible(context: Context): List<ApkSplit> {
		return asSequence().filterIncompatible(context).toList()
	}

	@JvmStatic
	public fun Array<ApkSplit>.filterIncompatible(context: Context): Array<ApkSplit> {
		return asSequence().filterIncompatible(context).toList().toTypedArray()
	}
}