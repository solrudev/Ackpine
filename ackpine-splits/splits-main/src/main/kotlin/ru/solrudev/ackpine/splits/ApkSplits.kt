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

import ru.solrudev.ackpine.exceptions.ConflictingBaseApkException
import ru.solrudev.ackpine.exceptions.ConflictingPackageNameException
import ru.solrudev.ackpine.exceptions.ConflictingSplitNameException
import ru.solrudev.ackpine.exceptions.ConflictingVersionCodeException
import ru.solrudev.ackpine.exceptions.NoBaseApkException
import ru.solrudev.ackpine.exceptions.SplitPackageException
import ru.solrudev.ackpine.helpers.closeWithException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Utilities for [sequences][Sequence] of [APK splits][Apk].
 */
public object ApkSplits {

	/**
	 * Returns a sequence which throws [SplitPackageException] on iteration if the split package is invalid and closes
	 * all I/O resources opened in the upstream sequence if it's [supported][CloseableSequence].
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
	 * This sequence supports cancellation when used with [SplitPackage] API.
	 *
	 * The operation is _intermediate_ and _stateful_.
	 *
	 * @return [CloseableSequence]
	 */
	@JvmStatic
	public fun Sequence<Apk>.validate(): CloseableSequence<Apk> {
		if (this is SplitPackageSequence) {
			return this
		}
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
	public fun Iterable<Apk>.validate(): List<Apk> {
		return asSequence().validate().toList()
	}
}

/**
 * A sequence which performs validation of split package and throws [SplitPackageException] if it's not valid.
 * This sequence handles closing of any resources held in upstream sequence when terminating with failure.
 */
private class SplitPackageSequence(
	private val source: Sequence<Apk>,
	private vararg val propertyCheckers: ApkPropertyChecker<*>
) : CloseableSequence<Apk> {

	@Volatile
	override var isClosed = false

	override fun iterator() = object : Iterator<Apk> {

		private val iterator = source.iterator()
		private var seenBaseApk = false
		private val splitNames = hashSetOf<String>()

		override fun hasNext() = iterator.hasNext()

		override fun next(): Apk {
			if (isClosed) {
				throw CancellationException("The sequence was closed")
			}
			val apk = iterator.next()
			if (!splitNames.add(apk.name)) {
				closeSource(ConflictingSplitNameException(apk.name))
			}
			if (apk is Apk.Base) {
				if (seenBaseApk) {
					closeSource(ConflictingBaseApkException())
				}
				seenBaseApk = true
			}
			for (propertyChecker in propertyCheckers) {
				propertyChecker.check(apk).onFailure(::closeSource)
			}
			if (!hasNext() && !seenBaseApk) {
				closeSource(NoBaseApkException())
			}
			return apk
		}
	}

	override fun close() {
		isClosed = true
		if (source is CloseableSequence) {
			source.close()
		}
	}

	private fun closeSource(exception: Throwable): Nothing {
		if (source is CloseableSequence) {
			source.closeWithException(exception)
		}
		throw exception
	}
}

private class ApkPropertyChecker<Property>(
	private val propertySelector: (Apk) -> Property,
	private val conflictingPropertyExceptionInitializer:
		(expected: Property, actual: Property, name: String) -> SplitPackageException
) {

	private var baseApkProperty: Property? = null
	private val propertyValues = mutableListOf<Property>()

	fun check(apk: Apk): Result<Unit> {
		val apkProperty = propertySelector(apk)
		if (apk is Apk.Base) {
			baseApkProperty = apkProperty
		}
		val expectedProperty = baseApkProperty
		if (expectedProperty != null) {
			if (expectedProperty != apkProperty) {
				return Result.failure(conflictingPropertyExceptionInitializer(expectedProperty, apkProperty, apk.name))
			}
			for (property in propertyValues) {
				if (expectedProperty != property) {
					return Result.failure(conflictingPropertyExceptionInitializer(expectedProperty, property, apk.name))
				}
			}
			propertyValues.clear()
		} else {
			propertyValues += apkProperty
		}
		return Result.success(Unit)
	}
}