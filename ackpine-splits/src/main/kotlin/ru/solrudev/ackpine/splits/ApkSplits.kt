/*
 * Copyright (C) 2023 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.solrudev.ackpine.splits

import android.content.Context
import ru.solrudev.ackpine.exceptions.ConflictingBaseApkException
import ru.solrudev.ackpine.exceptions.ConflictingPackageNameException
import ru.solrudev.ackpine.exceptions.ConflictingSplitNameException
import ru.solrudev.ackpine.exceptions.ConflictingVersionCodeException
import ru.solrudev.ackpine.exceptions.NoBaseApkException
import ru.solrudev.ackpine.exceptions.SplitPackageException
import ru.solrudev.ackpine.helpers.deviceLocales
import java.util.Locale
import kotlin.math.abs

/**
 * Utilities for [sequences][Sequence] of [APK splits][Apk].
 */
public object ApkSplits {

	/**
	 * Returns a sequence containing only [APK splits][Apk] which are most compatible with the device.
	 * If exact device's [screen density][Dpi], [ABI][Abi] or [locale][Locale] doesn't appear in the splits, nearest
	 * matching split is chosen.
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 *
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<Apk>.filterCompatible(context: Context): Sequence<Apk> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			val libsSplits = mutableListOf<Apk.Libs>()
			val densitySplits = mutableListOf<Apk.ScreenDensity>()
			val localizationSplits = mutableListOf<Apk.Localization>()
			this@filterCompatible
				.addSplitsOfTypeTo(libsSplits)
				.addSplitsOfTypeTo(densitySplits)
				.addSplitsOfTypeTo(localizationSplits)
				.filter { apk -> apk is Apk.Base || apk is Apk.Feature || apk is Apk.Other }
				.forEach { yield(it) }
			val deviceDensity = applicationContext.resources.displayMetrics.densityDpi
			val deviceLanguages = deviceLocales(applicationContext).map { it.language }
			libsSplits
				.filter { apk -> apk.abi in Abi.deviceAbis }
				.minByOrNull { apk -> Abi.deviceAbis.indexOf(apk.abi) }
				?.let { yield(it) }
			densitySplits
				.minByOrNull { apk -> abs(deviceDensity - apk.dpi.density) }
				?.let { yield(it) }
			localizationSplits
				.filter { apk -> apk.locale.language in deviceLanguages }
				.minByOrNull { apk -> deviceLanguages.indexOf(apk.locale.language) }
				?.let { yield(it) }
		}
	}

	/**
	 * Returns a sequence which throws [SplitPackageException] on iteration if the split package is invalid.
	 *
	 * If any [APK split][Apk] conflicts with [base APK][Apk.Base] by package name, [ConflictingPackageNameException]
	 * will be thrown. If any APK split conflicts with base APK by version code, [ConflictingVersionCodeException] will
	 * be thrown.
	 *
	 * If there is more than one base APK in the sequence, [ConflictingBaseApkException] will be thrown. If there is no
	 * base APK in the sequence, [NoBaseApkException] will be thrown.
	 *
	 * If there are conflicting split names, [ConflictingSplitNameException] will be thrown.
	 *
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<Apk>.throwOnInvalidSplitPackage(): Sequence<Apk> {
		return throwOnConflictingProperty(
			exceptionInitializer = ::ConflictingPackageNameException,
			propertySelector = Apk::packageName
		).throwOnConflictingProperty(
			exceptionInitializer = ::ConflictingVersionCodeException,
			propertySelector = Apk::versionCode
		)
	}

	/**
	 * Returns a list containing only [APK splits][Apk] which are most compatible with the device.
	 * If exact device's [screen density][Dpi], [ABI][Abi] or [locale][Locale] doesn't appear in the splits, nearest
	 * matching split is chosen.
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 */
	@JvmStatic
	public fun Iterable<Apk>.filterCompatible(context: Context): List<Apk> {
		return asSequence().filterCompatible(context).toList()
	}

	/**
	 * Returns a list of [APK splits][Apk] and throws [SplitPackageException] if the split package is invalid.
	 *
	 * If any [APK split][Apk] conflicts with [base APK][Apk.Base] by package name, [ConflictingPackageNameException]
	 * will be thrown. If any APK split conflicts with base APK by version code, [ConflictingVersionCodeException] will
	 * be thrown.
	 *
	 * If there is more than one base APK in the iterable, [ConflictingBaseApkException] will be thrown. If there is no
	 * base APK in the iterable, [NoBaseApkException] will be thrown.
	 *
	 * If there are conflicting split names, [ConflictingSplitNameException] will be thrown.
	 */
	@JvmStatic
	public fun Iterable<Apk>.throwOnInvalidSplitPackage(): List<Apk> {
		return asSequence().throwOnInvalidSplitPackage().toList()
	}

	private inline fun <reified SplitType : Apk> Sequence<Apk>.addSplitsOfTypeTo(
		splits: MutableList<SplitType>
	): Sequence<Apk> = onEach { apk ->
		if (apk is SplitType) {
			splits += apk
		}
	}

	/**
	 * Returns a sequence which throws on iteration if any [APK split][Apk] conflicts with [base APK][Apk.Base] by
	 * property specified with [propertySelector].
	 *
	 * If there is more than one base APK in the sequence, [ConflictingBaseApkException] will be thrown. If there is no
	 * base APK in the sequence, [NoBaseApkException] will be thrown.
	 *
	 * If there are conflicting split names, [ConflictingSplitNameException] will be thrown.
	 *
	 * The operation is _intermediate_ and _stateful_.
	 */
	private fun <Property> Sequence<Apk>.throwOnConflictingProperty(
		exceptionInitializer: (expected: Property, actual: Property, name: String) -> Exception,
		propertySelector: (Apk) -> Property
	): Sequence<Apk> {
		return SplitPackageSequence(this, exceptionInitializer, propertySelector)
	}
}

private class SplitPackageSequence<Property>(
	private val sequence: Sequence<Apk>,
	private val exceptionInitializer: (expected: Property, actual: Property, name: String) -> Exception,
	private val propertySelector: (Apk) -> Property
) : Sequence<Apk> {

	override fun iterator() = object : Iterator<Apk> {

		private val iterator = sequence.iterator()
		private var seenBaseApk = false
		private var baseApkProperty: Property? = null
		private val propertyValues = mutableListOf<Property>()
		private val splitNames = hashSetOf<String>()

		override fun hasNext() = iterator.hasNext()

		override fun next(): Apk {
			val apk = iterator.next()
			if (!splitNames.add(apk.name)) {
				throw ConflictingSplitNameException(apk.name)
			}
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
				checkApkProperty(expectedProperty, apkProperty, apk.name)
				propertyValues.forEach { property ->
					checkApkProperty(expectedProperty, property, apk.name)
				}
				propertyValues.clear()
			} else {
				propertyValues += apkProperty
			}
			if (!hasNext() && !seenBaseApk) {
				throw NoBaseApkException()
			}
			return apk
		}

		private fun checkApkProperty(baseProperty: Property, apkProperty: Property, name: String) {
			if (baseProperty != apkProperty) {
				throw exceptionInitializer(baseProperty, apkProperty, name)
			}
		}
	}
}