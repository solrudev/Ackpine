package ru.solrudev.ackpine

import android.content.Context
import ru.solrudev.ackpine.exceptions.ConflictingBaseApkException
import ru.solrudev.ackpine.exceptions.ConflictingPackageNameException
import ru.solrudev.ackpine.exceptions.ConflictingVersionCodeException
import ru.solrudev.ackpine.helpers.deviceLocales
import kotlin.math.abs

public object ApkSplits {

	/**
	 * The operation is _intermediate_ and _stateful_.
	 */
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
				.filter { apk -> apk is ApkSplit.Base || apk is ApkSplit.Feature || apk is ApkSplit.Other }
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

	/**
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<ApkSplit>.throwOnConflictingPackageName(): Sequence<ApkSplit> {
		return throwOnConflictingProperty(
			exceptionInitializer = ::ConflictingPackageNameException,
			propertySelector = { apk -> apk.packageName }
		)
	}

	/**
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<ApkSplit>.throwOnConflictingVersionCode(): Sequence<ApkSplit> {
		return throwOnConflictingProperty(
			exceptionInitializer = ::ConflictingVersionCodeException,
			propertySelector = { apk -> apk.versionCode }
		)
	}

	/**
	 * Shortcut for
	 * [throwOnConflictingPackageName()][throwOnConflictingPackageName]`.`[throwOnConflictingVersionCode()][throwOnConflictingVersionCode].
	 *
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<ApkSplit>.throwOnConflictingPackageNameOrVersionCode(): Sequence<ApkSplit> {
		return throwOnConflictingPackageName().throwOnConflictingVersionCode()
	}

	@JvmStatic
	public fun Iterable<ApkSplit>.filterIncompatible(context: Context): List<ApkSplit> {
		return asSequence().filterIncompatible(context).toList()
	}

	@JvmStatic
	public fun Iterable<ApkSplit>.throwOnConflictingPackageName(): List<ApkSplit> {
		return asSequence().throwOnConflictingPackageName().toList()
	}

	@JvmStatic
	public fun Iterable<ApkSplit>.throwOnConflictingVersionCode(): List<ApkSplit> {
		return asSequence().throwOnConflictingVersionCode().toList()
	}

	@JvmStatic
	public fun Iterable<ApkSplit>.throwOnConflictingPackageNameOrVersionCode(): List<ApkSplit> {
		return asSequence().throwOnConflictingPackageNameOrVersionCode().toList()
	}

	private inline fun <reified SplitType : ApkSplit> Sequence<ApkSplit>.addSplitsOfTypeTo(
		splits: MutableList<SplitType>,
		applicationContext: Context
	): Sequence<ApkSplit> = onEach { apk ->
		if (apk is SplitType && apk.isCompatible(applicationContext)) {
			splits += apk
		}
	}

	/**
	 * The operation is _intermediate_ and _stateful_.
	 */
	private inline fun <reified Property> Sequence<ApkSplit>.throwOnConflictingProperty(
		crossinline exceptionInitializer: (expected: Property, actual: Property, name: String) -> Exception,
		crossinline propertySelector: (ApkSplit) -> Property
	): Sequence<ApkSplit> {
		var seenBaseApk = false
		var baseApkProperty: Property? = null
		val propertyValues = mutableListOf<Property>()
		return onEach { apk ->
			val apkProperty = propertySelector(apk)
			if (apk is ApkSplit.Base) {
				if (seenBaseApk) {
					throw ConflictingBaseApkException()
				}
				seenBaseApk = true
				baseApkProperty = apkProperty
			}
			val expectedProperty = baseApkProperty
			if (expectedProperty != null) {
				checkApkProperty(expectedProperty, apkProperty, apk.name, exceptionInitializer)
				propertyValues.forEach { property ->
					checkApkProperty(expectedProperty, property, apk.name, exceptionInitializer)
				}
				propertyValues.clear()
			} else {
				propertyValues += apkProperty
			}
		}
	}

	private inline fun <Property> checkApkProperty(
		baseProperty: Property,
		apkProperty: Property,
		name: String,
		exceptionInitializer: (expected: Property, actual: Property, name: String) -> Exception
	) {
		if (baseProperty != apkProperty) {
			throw exceptionInitializer(baseProperty, apkProperty, name)
		}
	}
}