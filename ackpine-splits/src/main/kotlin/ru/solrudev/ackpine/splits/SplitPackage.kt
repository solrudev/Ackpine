/*
 * Copyright (C) 2025 Ilya Fomichev
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
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.AckpineThreadPool
import ru.solrudev.ackpine.helpers.ImmediateListenableFuture
import ru.solrudev.ackpine.helpers.concurrent.map
import ru.solrudev.ackpine.helpers.onCancellation
import ru.solrudev.ackpine.splits.SplitPackage.Provider
import ru.solrudev.ackpine.splits.helpers.deviceLocales
import java.util.Locale
import kotlin.math.abs

/**
 * An [APK split][Apk] package.
 */
public open class SplitPackage(

	/**
	 * A list of all [base][Apk.Base] APKs inside of the split package.
	 *
	 * If there's more than one base APK, split package is invalid.
	 */
	public val base: List<Entry<Apk.Base>>,

	/**
	 * A list of all [APKs with native libraries][Apk.Libs] inside of the split package.
	 */
	public val libs: List<Entry<Apk.Libs>>,

	/**
	 * A list of all [APKs with graphic resources tailored for specific screen density][Apk.ScreenDensity] inside of the
	 * split package.
	 */
	public val screenDensity: List<Entry<Apk.ScreenDensity>>,

	/**
	 * A list of all [APKs with localized resources][Apk.Localization] inside of the split package.
	 */
	public val localization: List<Entry<Apk.Localization>>,

	/**
	 * A list of all [unknown APK splits][Apk.Other] inside of the split package.
	 */
	public val other: List<Entry<Apk.Other>>,

	/**
	 * A list of all [dynamic features][DynamicFeature] inside of the split package.
	 */
	public val dynamicFeatures: List<DynamicFeature>
) {

	/**
	 * [APK splits][Apk] of a dynamic feature.
	 */
	public data class DynamicFeature(

		/**
		 * A [base APK of the dynamic feature][Apk.Feature].
		 */
		public val feature: Apk.Feature,

		/**
		 * A list of all [APKs with native libraries][Apk.Libs] needed for the dynamic feature.
		 */
		public val libs: List<Entry<Apk.Libs>>,

		/**
		 * A list of all [APKs with graphic resources tailored for specific screen density][Apk.ScreenDensity] needed for
		 * the dynamic feature.
		 */
		public val screenDensity: List<Entry<Apk.ScreenDensity>>,

		/**
		 * A list of all [APKs with localized resources][Apk.Localization] needed for the dynamic feature.
		 */
		public val localization: List<Entry<Apk.Localization>>
	)

	/**
	 * A [SplitPackage] entry.
	 *
	 * @property isPreferred indicates whether the [apk] is the most preferred for the device among all splits of the
	 * same type. By default it is `true`.
	 * @property apk an [APK split][Apk].
	 */
	public data class Entry<out T : Apk>(
		public val isPreferred: Boolean,
		public val apk: T
	)

	/**
	 * Returns a [SplitPackage] containing only [APK splits][Apk] which are the most preferred for the device by
	 * examining every [entry's][Entry] [isPreferred][Entry.isPreferred] flag.
	 */
	public fun filterPreferred(): SplitPackage {
		if (this is FilteredSplitPackage) {
			return this
		}
		val libs = libs.filter { it.isPreferred }
		val density = screenDensity.filter { it.isPreferred }
		val localization = localization.filter { it.isPreferred }
		val features = dynamicFeatures.map { feature ->
			val featureLibs = feature.libs.filter { it.isPreferred }
			val featureDensity = feature.screenDensity.filter { it.isPreferred }
			val featureLocalization = feature.localization.filter { it.isPreferred }
			DynamicFeature(feature.feature, featureLibs, featureDensity, featureLocalization)
		}
		return FilteredSplitPackage(base, libs, density, localization, other, features)
	}

	/**
	 * Returns a new list populated with all [APK splits][Apk] contained in this split package.
	 */
	public fun toList(): List<Entry<*>> {
		return buildList {
			addAll(base)
			addAll(libs)
			addAll(screenDensity)
			addAll(localization)
			addAll(other)
			for (feature in dynamicFeatures) {
				add(Entry(isPreferred = true, feature.feature))
				addAll(feature.libs)
				addAll(feature.screenDensity)
				addAll(feature.localization)
			}
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as SplitPackage
		if (base != other.base) return false
		if (libs != other.libs) return false
		if (screenDensity != other.screenDensity) return false
		if (localization != other.localization) return false
		if (other != other.other) return false
		if (dynamicFeatures != other.dynamicFeatures) return false
		return true
	}

	override fun hashCode(): Int {
		var result = base.hashCode()
		result = 31 * result + libs.hashCode()
		result = 31 * result + screenDensity.hashCode()
		result = 31 * result + localization.hashCode()
		result = 31 * result + other.hashCode()
		result = 31 * result + dynamicFeatures.hashCode()
		return result
	}

	override fun toString(): String {
		return "SplitPackage(" +
				"base=$base, " +
				"libs=$libs, " +
				"screenDensity=$screenDensity, " +
				"localization=$localization, " +
				"other=$other, " +
				"dynamicFeatures=$dynamicFeatures" +
				")"
	}

	/**
	 * A lazy provider of [SplitPackage] which also allows to form a transformation pipeline before actual creation will
	 * take place.
	 */
	public fun interface Provider {

		/**
		 * Creates a [SplitPackage] with transformations applied via this provider.
		 *
		 * This future may be cancelled if the split package source supports it (such as [CloseableSequence]).
		 *
		 * The resulting [SplitPackage] is not cached.
		 */
		public fun getAsync(): ListenableFuture<SplitPackage>

		/**
		 * Returns a [Provider] giving out only [APK splits][Apk] which are the most compatible with the device by
		 * applying [sortedByCompatibility] operation and calling [filterPreferred] on the resulting [SplitPackage].
		 *
		 * If exact device's [screen density][Dpi], [ABI][Abi] or [locale][Locale] doesn't appear in the splits, nearest
		 * matching split is chosen.
		 *
		 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
		 */
		public fun filterCompatible(context: Context): Provider {
			return when (this) {
				EmptyProvider,
				is FilteringProvider -> this

				is SortingProvider -> FilteringProvider(provider = this)
				else -> FilteringProvider(SortingProvider(provider = this, context.applicationContext))
			}
		}

		/**
		 * Returns a [Provider] giving out [APK splits][Apk] sorted according to their compatibility with the device.
		 *
		 * This sort is _stable_ for each APK split type.
		 *
		 * The most preferred APK splits will appear first. If exact device's [screen density][Dpi], [ABI][Abi] or
		 * [locale][Locale] doesn't appear in the splits, nearest matching split is chosen as a preferred one.
		 *
		 * Result of preference evaluation for every [entry][Entry] of this Provider is written to their
		 * [isPreferred][Entry.isPreferred] flag.
		 *
		 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
		 */
		public fun sortedByCompatibility(context: Context): Provider {
			return when (this) {
				EmptyProvider,
				is SortingProvider,
				is FilteringProvider -> this

				else -> SortingProvider(provider = this, context.applicationContext)
			}
		}
	}

	private class FilteringProvider(private val provider: Provider) : Provider {
		override fun getAsync(): ListenableFuture<SplitPackage> {
			return provider.getAsync().map(SplitPackage::filterPreferred)
		}
	}

	private class SortingProvider(
		private val provider: Provider,
		private val context: Context
	) : Provider {

		override fun getAsync(): ListenableFuture<SplitPackage> {
			return provider.getAsync().map(::sortedByCompatibility)
		}

		private fun sortedByCompatibility(splitPackage: SplitPackage): SplitPackage {
			if (splitPackage is SortedSplitPackage || splitPackage is FilteredSplitPackage) {
				return splitPackage
			}
			val deviceDensity = context.resources.displayMetrics.densityDpi
			val deviceLanguages = deviceLocales(context).map { it.language }
			val libs = splitPackage
				.libs
				.sortedByCompatibility(
					isCompatible = { apk -> apk.abi in Abi.deviceAbis },
					selector = ::libsComparator
				)
			val density = splitPackage
				.screenDensity
				.sortedByCompatibility { apk -> densityComparator(apk, deviceDensity) }
			val localization = splitPackage
				.localization
				.sortedByCompatibility(
					isCompatible = { apk -> apk.locale.language in deviceLanguages },
					selector = { apk -> localizationComparator(apk, deviceLanguages) }
				)
			val features = splitPackage.dynamicFeatures.map { feature ->
				val featureLibs = feature
					.libs
					.sortedByCompatibility(
						isCompatible = { apk -> apk.abi in Abi.deviceAbis },
						selector = ::libsComparator
					)
				val featureDensity = feature
					.screenDensity
					.sortedByCompatibility { apk -> densityComparator(apk, deviceDensity) }
				val featureLocalization = feature
					.localization
					.sortedByCompatibility(
						isCompatible = { apk -> apk.locale.language in deviceLanguages },
						selector = { apk -> localizationComparator(apk, deviceLanguages) }
					)
				DynamicFeature(feature.feature, featureLibs, featureDensity, featureLocalization)
			}
			return SortedSplitPackage(splitPackage.base, libs, density, localization, splitPackage.other, features)
		}

		private inline fun <T : Apk, R : Comparable<R>> List<Entry<T>>.sortedByCompatibility(
			crossinline isCompatible: (T) -> Boolean = { true },
			crossinline selector: (T) -> R?
		) = sortedBy { entry -> selector(entry.apk) }
			.mapIndexed { index, entry ->
				Entry(isPreferred = index == 0 && isCompatible(entry.apk), entry.apk)
			}

		private fun libsComparator(apk: Apk.Libs): Int {
			val index = Abi.deviceAbis.indexOf(apk.abi)
			return if (index == -1) Int.MAX_VALUE else index
		}

		private fun localizationComparator(apk: Apk.Localization, deviceLanguages: List<String>): Int {
			val index = deviceLanguages.indexOf(apk.locale.language)
			return if (index == -1) Int.MAX_VALUE else index
		}

		private fun densityComparator(apk: Apk.ScreenDensity, deviceDensity: Int): Int {
			return abs(deviceDensity - apk.dpi.density)
		}
	}

	private object EmptyProvider : Provider {
		override fun getAsync(): ListenableFuture<SplitPackage> {
			return ImmediateListenableFuture(EmptySplitPackage)
		}
	}

	/**
	 * This companion object contains factories of [SplitPackage.Provider].
	 */
	public companion object {

		/**
		 * Returns a [Provider] which creates an empty [SplitPackage].
		 */
		@JvmStatic
		public fun empty(): Provider {
			return EmptyProvider
		}

		/**
		 * Returns a [Provider] which iterates over the [APK splits][Apk] [sequence][Sequence] and creates a
		 * [SplitPackage] containing all APK splits from this sequence.
		 */
		@JvmStatic
		@JvmName("from")
		public fun Sequence<Apk>.toSplitPackage(): Provider {
			val source = this
			return SplitPackageProvider { completer ->
				completer.onCancellation {
					if (source is CloseableSequence) {
						source.close()
					}
				}
				AckpineThreadPool.executor.execute {
					try {
						completer.set(createSplitPackage(source))
					} catch (exception: Exception) {
						completer.setException(exception)
					}
				}
			}
		}

		private fun createSplitPackage(source: Sequence<Apk>): SequenceSplitPackage {
			val base = mutableListOf<Entry<Apk.Base>>()
			val libs = mutableListOf<Entry<Apk.Libs>>()
			val density = mutableListOf<Entry<Apk.ScreenDensity>>()
			val localization = mutableListOf<Entry<Apk.Localization>>()
			val other = mutableListOf<Entry<Apk.Other>>()
			val features = mutableListOf<Apk.Feature>()
			for (apk in source) {
				when (apk) {
					is Apk.Base -> base += Entry(isPreferred = true, apk)
					is Apk.Libs -> libs += Entry(isPreferred = true, apk)
					is Apk.Localization -> localization += Entry(isPreferred = true, apk)
					is Apk.Other -> other += Entry(isPreferred = true, apk)
					is Apk.ScreenDensity -> density += Entry(isPreferred = true, apk)
					is Apk.Feature -> features += apk
				}
			}
			val featureLibsMap = libs.groupBy { it.apk.configForSplit }
			val featureDensityMap = density.groupBy { it.apk.configForSplit }
			val featureLocalizationMap = localization.groupBy { it.apk.configForSplit }
			val dynamicFeatures = features.map { feature ->
				val featureLibs = featureLibsMap[feature.name].orEmpty()
				val featureDensity = featureDensityMap[feature.name].orEmpty()
				val featureLocalization = featureLocalizationMap[feature.name].orEmpty()
				libs.removeAll(featureLibs)
				density.removeAll(featureDensity)
				localization.removeAll(featureLocalization)
				DynamicFeature(feature, featureLibs, featureDensity, featureLocalization)
			}
			return SequenceSplitPackage(base, libs, density, localization, other, dynamicFeatures)
		}

		/**
		 * Returns a [Provider] which iterates over the [APK splits][Apk] and creates a [SplitPackage] containing all
		 * APK splits from this iterable.
		 */
		@JvmStatic
		@JvmName("from")
		public fun Iterable<Apk>.toSplitPackage(): Provider {
			return asSequence().toSplitPackage()
		}
	}

	private class SequenceSplitPackage(
		base: List<Entry<Apk.Base>>,
		libs: List<Entry<Apk.Libs>>,
		screenDensity: List<Entry<Apk.ScreenDensity>>,
		localization: List<Entry<Apk.Localization>>,
		other: List<Entry<Apk.Other>>,
		dynamicFeatures: List<DynamicFeature>
	) : SplitPackage(base, libs, screenDensity, localization, other, dynamicFeatures)

	private class SortedSplitPackage(
		base: List<Entry<Apk.Base>>,
		libs: List<Entry<Apk.Libs>>,
		screenDensity: List<Entry<Apk.ScreenDensity>>,
		localization: List<Entry<Apk.Localization>>,
		other: List<Entry<Apk.Other>>,
		dynamicFeatures: List<DynamicFeature>
	) : SplitPackage(base, libs, screenDensity, localization, other, dynamicFeatures)

	private class FilteredSplitPackage(
		base: List<Entry<Apk.Base>>,
		libs: List<Entry<Apk.Libs>>,
		screenDensity: List<Entry<Apk.ScreenDensity>>,
		localization: List<Entry<Apk.Localization>>,
		other: List<Entry<Apk.Other>>,
		dynamicFeatures: List<DynamicFeature>
	) : SplitPackage(base, libs, screenDensity, localization, other, dynamicFeatures)

	private object EmptySplitPackage : SplitPackage(
		emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
	)
}

@Suppress("FunctionName")
private inline fun SplitPackageProvider(
	crossinline block: (CallbackToFutureAdapter.Completer<SplitPackage>) -> Unit
) = Provider {
	CallbackToFutureAdapter.getFuture { completer ->
		try {
			block(completer)
		} catch (exception: Exception) {
			completer.setException(exception)
		}
		"SplitPackageProvider"
	}
}