package ru.solrudev.ackpine.splits

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
	public fun Sequence<Apk>.filterIncompatible(context: Context): Sequence<Apk> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			val libsSplits = mutableListOf<Apk.Libs>()
			val densitySplits = mutableListOf<Apk.ScreenDensity>()
			val localizationSplits = mutableListOf<Apk.Localization>()
			this@filterIncompatible
				.addSplitsOfTypeTo(libsSplits, applicationContext)
				.addSplitsOfTypeTo(densitySplits, applicationContext)
				.addSplitsOfTypeTo(localizationSplits, applicationContext)
				.filter { apk -> apk is Apk.Base || apk is Apk.Feature || apk is Apk.Other }
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
	public fun Sequence<Apk>.throwOnConflictingPackageName(): Sequence<Apk> {
		return throwOnConflictingProperty(
			exceptionInitializer = ::ConflictingPackageNameException,
			propertySelector = Apk::packageName
		)
	}

	/**
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<Apk>.throwOnConflictingVersionCode(): Sequence<Apk> {
		return throwOnConflictingProperty(
			exceptionInitializer = ::ConflictingVersionCodeException,
			propertySelector = Apk::versionCode
		)
	}

	/**
	 * Shortcut for
	 * [throwOnConflictingPackageName()][throwOnConflictingPackageName]`.`[throwOnConflictingVersionCode()][throwOnConflictingVersionCode].
	 *
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<Apk>.throwOnConflictingPackageNameOrVersionCode(): Sequence<Apk> {
		return throwOnConflictingPackageName().throwOnConflictingVersionCode()
	}

	@JvmStatic
	public fun Iterable<Apk>.filterIncompatible(context: Context): List<Apk> {
		return asSequence().filterIncompatible(context).toList()
	}

	@JvmStatic
	public fun Iterable<Apk>.throwOnConflictingPackageName(): List<Apk> {
		return asSequence().throwOnConflictingPackageName().toList()
	}

	@JvmStatic
	public fun Iterable<Apk>.throwOnConflictingVersionCode(): List<Apk> {
		return asSequence().throwOnConflictingVersionCode().toList()
	}

	@JvmStatic
	public fun Iterable<Apk>.throwOnConflictingPackageNameOrVersionCode(): List<Apk> {
		return asSequence().throwOnConflictingPackageNameOrVersionCode().toList()
	}

	private inline fun <reified SplitType : Apk> Sequence<Apk>.addSplitsOfTypeTo(
		splits: MutableList<SplitType>,
		applicationContext: Context
	): Sequence<Apk> = onEach { apk ->
		if (apk is SplitType && apk.isCompatible(applicationContext)) {
			splits += apk
		}
	}

	/**
	 * The operation is _intermediate_ and _stateful_.
	 */
	private inline fun <reified Property> Sequence<Apk>.throwOnConflictingProperty(
		crossinline exceptionInitializer: (expected: Property, actual: Property, name: String) -> Exception,
		crossinline propertySelector: (Apk) -> Property
	): Sequence<Apk> {
		var seenBaseApk = false
		var baseApkProperty: Property? = null
		val propertyValues = mutableListOf<Property>()
		return onEach { apk ->
			val apkProperty = propertySelector(apk)
			if (apk is Apk.Base) {
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