/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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
import ru.solrudev.ackpine.splits.helpers.deviceLocales
import java.util.Locale
import kotlin.math.abs

/**
 * Utilities for [sequences][Sequence] of [APK splits][Apk].
 */
public object ApkSplits {

	/**
	 * Returns a sequence that yields [APK splits][Apk] sorted according to their compatibility with the device.
	 *
	 * This sort is _not stable_.
	 *
	 * The most preferred APK splits will appear first. If exact device's [screen density][Dpi], [ABI][Abi] or
	 * [locale][Locale] doesn't appear in the splits, nearest matching split is chosen as a preferred one.
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 *
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<Apk>.sortedByCompatibility(context: Context): Sequence<ApkCompatibility> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return sequence {
			val libsSplits = mutableListOf<Apk.Libs>()
			val densitySplits = mutableListOf<Apk.ScreenDensity>()
			val localizationSplits = mutableListOf<Apk.Localization>()
			this@sortedByCompatibility
				.addSplitsOfTypeTo(libsSplits)
				.addSplitsOfTypeTo(densitySplits)
				.addSplitsOfTypeTo(localizationSplits)
				.filter { apk -> apk is Apk.Base || apk is Apk.Feature || apk is Apk.Other }
				.forEach { yield(ApkCompatibility(isPreferred = true, it)) }
			val deviceDensity = applicationContext.resources.displayMetrics.densityDpi
			val deviceLanguages = deviceLocales(applicationContext).map { it.language }
			libsSplits.sortBy { apk ->
				val index = Abi.deviceAbis.indexOf(apk.abi)
				if (index == -1) Int.MAX_VALUE else index
			}
			libsSplits.firstOrNull()
				?.takeIf { apk -> apk.abi in Abi.deviceAbis }
				?.also { libsSplits -= it }
				?.let { yield(ApkCompatibility(isPreferred = true, it)) }
			densitySplits.sortBy { apk -> abs(deviceDensity - apk.dpi.density) }
			densitySplits.firstOrNull()
				?.also { densitySplits -= it }
				?.let { yield(ApkCompatibility(isPreferred = true, it)) }
			localizationSplits.sortBy { apk ->
				val index = deviceLanguages.indexOf(apk.locale.language)
				if (index == -1) Int.MAX_VALUE else index
			}
			localizationSplits.firstOrNull()
				?.takeIf { apk -> apk.locale.language in deviceLanguages }
				?.also { localizationSplits -= it }
				?.let { yield(ApkCompatibility(isPreferred = true, it)) }
			for (apk in libsSplits) {
				yield(ApkCompatibility(isPreferred = false, apk))
			}
			for (apk in densitySplits) {
				yield(ApkCompatibility(isPreferred = false, apk))
			}
			for (apk in localizationSplits) {
				yield(ApkCompatibility(isPreferred = false, apk))
			}
		}
	}

	/**
	 * Returns a sequence containing only [APK splits][Apk] which are the most compatible with the device.
	 *
	 * If exact device's [screen density][Dpi], [ABI][Abi] or [locale][Locale] doesn't appear in the splits, nearest
	 * matching split is chosen.
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 *
	 * The operation is _intermediate_ and _stateful_.
	 */
	@JvmStatic
	public fun Sequence<Apk>.filterCompatible(context: Context): Sequence<Apk> {
		return sortedByCompatibility(context)
			.filter { it.isPreferred }
			.map { it.apk }
	}

	/**
	 * Returns a sequence containing only [APK splits][Apk] which are the most compatible with the device.
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 *
	 * The operation is _intermediate_ and _stateless_.
	 */
	@JvmStatic
	public fun Sequence<ApkCompatibility>.filterCompatible(): Sequence<Apk> {
		return filter { it.isPreferred }
			.map { it.apk }
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
		return SplitPackageSequence(
			source = this,
			ApkPropertyChecker(
				propertySelector = Apk::packageName,
				conflictingPropertyExceptionInitializer = ::ConflictingPackageNameException
			),
			ApkPropertyChecker(
				propertySelector = Apk::versionCode,
				conflictingPropertyExceptionInitializer = ::ConflictingVersionCodeException
			)
		)
	}

	/**
	 * Returns a sequence which adds all elements to the [destination] list as they pass through.
	 *
	 * This operation is _intermediate_ and _stateless_.
	 */
	@JvmStatic
	public fun Sequence<ApkCompatibility>.addAllTo(
		destination: MutableList<ApkCompatibility>
	): Sequence<ApkCompatibility> {
		return onEach { destination += it }
	}

	/**
	 * Returns a list containing only [APK splits][Apk] which are the most compatible with the device.
	 *
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
	 * Returns a list containing [APK splits][Apk] sorted according to their compatibility with the device.
	 *
	 * This sort is _not stable_.
	 *
	 * The most preferred APK splits will appear first. If exact device's [screen density][Dpi], [ABI][Abi] or
	 * [locale][Locale] doesn't appear in the splits, nearest matching split is chosen as a preferred one.
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 */
	@JvmStatic
	public fun Iterable<Apk>.sortedByCompatibility(context: Context): List<ApkCompatibility> {
		return asSequence().sortedByCompatibility(context).toList()
	}

	/**
	 * Returns a list containing only [APK splits][Apk] which are the most compatible with the device.
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 */
	@JvmStatic
	public fun Iterable<ApkCompatibility>.filterCompatible(): List<Apk> {
		return asSequence().filterCompatible().toList()
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
}

private class SplitPackageSequence(
	private val source: Sequence<Apk>,
	private vararg val propertyCheckers: ApkPropertyChecker<*>
) : Sequence<Apk> {

	override fun iterator() = object : Iterator<Apk> {

		private val iterator = source.iterator()
		private var seenBaseApk = false
		private val splitNames = hashSetOf<String>()

		override fun hasNext() = iterator.hasNext()

		override fun next(): Apk {
			val apk = iterator.next()
			if (!splitNames.add(apk.name)) {
				throw ConflictingSplitNameException(apk.name)
			}
			if (apk is Apk.Base) {
				if (seenBaseApk) {
					throw ConflictingBaseApkException()
				}
				seenBaseApk = true
			}
			for (propertyChecker in propertyCheckers) {
				propertyChecker.checkApk(apk)
			}
			if (!hasNext() && !seenBaseApk) {
				throw NoBaseApkException()
			}
			return apk
		}
	}
}

private class ApkPropertyChecker<Property>(
	private val propertySelector: (Apk) -> Property,
	private val conflictingPropertyExceptionInitializer:
		(expected: Property, actual: Property, name: String) -> Exception
) {

	private var baseApkProperty: Property? = null
	private val propertyValues = mutableListOf<Property>()

	fun checkApk(apk: Apk) {
		val apkProperty = propertySelector(apk)
		if (apk is Apk.Base) {
			baseApkProperty = apkProperty
		}
		val expectedProperty = baseApkProperty
		if (expectedProperty != null) {
			checkProperty(expectedProperty, apkProperty, apk.name)
			for (property in propertyValues) {
				checkProperty(expectedProperty, property, apk.name)
			}
			propertyValues.clear()
		} else {
			propertyValues += apkProperty
		}
	}

	private fun checkProperty(baseProperty: Property, apkProperty: Property, name: String) {
		if (baseProperty != apkProperty) {
			throw conflictingPropertyExceptionInitializer(baseProperty, apkProperty, name)
		}
	}
}