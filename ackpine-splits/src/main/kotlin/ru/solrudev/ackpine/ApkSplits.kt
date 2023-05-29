package ru.solrudev.ackpine

import android.content.Context
import ru.solrudev.ackpine.helpers.deviceLocales
import kotlin.math.abs

public object ApkSplits {

	@JvmStatic
	public fun Sequence<ApkSplit>.filterIncompatible(context: Context): Sequence<ApkSplit> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			val libsSplits = mutableListOf<ApkSplit.Libs>()
			val densitySplits = mutableListOf<ApkSplit.ScreenDensity>()
			val localizationSplits = mutableListOf<ApkSplit.Localization>()
			this@filterIncompatible
				.addSplitsOfTypeTo(libsSplits, applicationContext)
				.addSplitsOfTypeTo(densitySplits, applicationContext)
				.addSplitsOfTypeTo(localizationSplits, applicationContext)
				.filter { apk -> apk is ApkSplit.Other }
				.forEach { yield(it) }
			val deviceDensity = applicationContext.resources.displayMetrics.densityDpi
			val deviceLanguages = deviceLocales(applicationContext).map { it.language }
			libsSplits.sortBy { apk -> Abi.deviceAbis.indexOf(apk.abi) }
			densitySplits.sortBy { apk -> abs(deviceDensity - apk.dpi.density) }
			localizationSplits.sortBy { apk -> deviceLanguages.indexOf(apk.locale.language).takeIf { it != -1 } }
			libsSplits.firstOrNull()?.let { yield(it) }
			densitySplits.firstOrNull()?.let { yield(it) }
			localizationSplits.firstOrNull()?.let { yield(it) }
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

	private inline fun <reified SplitType : ApkSplit> Sequence<ApkSplit>.addSplitsOfTypeTo(
		splits: MutableList<SplitType>,
		applicationContext: Context
	): Sequence<ApkSplit> = onEach { apk ->
		if (apk is SplitType && apk.isCompatible(applicationContext)) {
			splits += apk
		}
	}
}