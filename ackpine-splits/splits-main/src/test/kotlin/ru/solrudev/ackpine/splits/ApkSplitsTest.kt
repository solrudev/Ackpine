/*
 * Copyright (C) 2026 Ilya Fomichev
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

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.exceptions.ConflictingBaseApkException
import ru.solrudev.ackpine.exceptions.ConflictingPackageNameException
import ru.solrudev.ackpine.exceptions.ConflictingSplitNameException
import ru.solrudev.ackpine.exceptions.ConflictingVersionCodeException
import ru.solrudev.ackpine.exceptions.NoBaseApkException
import ru.solrudev.ackpine.splits.ApkSplits.validate
import ru.solrudev.ackpine.splits.testutil.createBaseApk
import ru.solrudev.ackpine.splits.testutil.createLibsApk
import ru.solrudev.ackpine.splits.testutil.createScreenDensityApk
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ApkSplitsTest {

	@Test
	fun validatePassesValidSequence() {
		val base = createBaseApk()
		val libs = createLibsApk()
		val density = createScreenDensityApk()
		val apks = sequenceOf(base, libs, density)

		val result = apks.validate().toList()

		assertEquals(listOf(base, libs, density), result)
	}

	@Test
	fun validateThrowsNoBaseApkExceptionWhenBaseIsAbsent() {
		val apks = sequenceOf(createLibsApk())
		assertFailsWith<NoBaseApkException> {
			apks.validate().toList()
		}
	}

	@Test
	fun validateAcceptsEmptySequence() {
		val result = emptySequence<Apk>().validate().toList()
		assertTrue(result.isEmpty())
	}

	@Test
	fun validateThrowsConflictingBaseApkExceptionOnMultipleBases() {
		val apks = sequenceOf(
			createBaseApk(name = "base"),
			createBaseApk(name = "base2")
		)
		assertFailsWith<ConflictingBaseApkException> {
			apks.validate().toList()
		}
	}

	@Test
	fun validateThrowsConflictingPackageNameExceptionOnDifferentPackageNames() {
		val apks = sequenceOf(
			createBaseApk(packageName = "com.example.app"),
			createLibsApk(packageName = "com.example.other")
		)
		val exception = assertFailsWith<ConflictingPackageNameException> {
			apks.validate().toList()
		}
		assertEquals("com.example.app", exception.expected)
		assertEquals("com.example.other", exception.actual)
	}

	@Test
	fun validateThrowsConflictingVersionCodeExceptionOnDifferentVersionCodes() {
		val apks = sequenceOf(
			createBaseApk(versionCode = 1),
			createLibsApk(versionCode = 2)
		)
		val exception = assertFailsWith<ConflictingVersionCodeException> {
			apks.validate().toList()
		}
		assertEquals(1L, exception.expected)
		assertEquals(2L, exception.actual)
	}

	@Test
	fun validateThrowsConflictingSplitNameExceptionOnNonUniqueSplitNames() {
		val apks = sequenceOf(
			createBaseApk(),
			createLibsApk(name = "config.arm64_v8a"),
			createLibsApk(name = "config.arm64_v8a", abi = Abi.ARMEABI_V7A)
		)
		val exception = assertFailsWith<ConflictingSplitNameException> {
			apks.validate().toList()
		}
		assertEquals("config.arm64_v8a", exception.name)
	}

	@Test
	fun validateDetectsConflictingPackageNameWhenBaseAppearsAfterOtherSplits() {
		val apks = sequenceOf(
			createLibsApk(packageName = "com.example.other"),
			createBaseApk(packageName = "com.example.app")
		)
		assertFailsWith<ConflictingPackageNameException> {
			apks.validate().toList()
		}
	}

	@Test
	fun validateDetectsConflictingVersionCodeWhenBaseAppearsAfterOtherSplits() {
		val apks = sequenceOf(
			createLibsApk(versionCode = 2),
			createBaseApk(versionCode = 1)
		)
		assertFailsWith<ConflictingVersionCodeException> {
			apks.validate().toList()
		}
	}

	@Test
	fun validateIsIdempotent() {
		val base = createBaseApk()
		val apks = sequenceOf(base)
		val validated = apks.validate()
		val validatedTwice = validated.validate()
		assertSame(validated, validatedTwice)
	}

	@Test
	fun validateClosePropagatesToSource() {
		val source = closeableSequence {
			yield(createBaseApk())
		}

		val validated = source.validate()
		validated.close()

		assertTrue(source.isClosed)
	}

	@Test
	fun validateClosesSourceOnValidationError() {
		val source = closeableSequence {
			yield(createLibsApk())
		}
		assertFailsWith<NoBaseApkException> {
			source.validate().toList()
		}
		assertTrue(source.isClosed)
	}

	@Test
	fun validateThrowsCancellationExceptionWhenClosed() {
		val source = closeableSequence {
			yield(createBaseApk())
			yield(createLibsApk())
		}

		val validated = source.validate()
		val iterator = validated.iterator()
		iterator.next()
		validated.close()

		assertFailsWith<CancellationException> {
			iterator.next()
		}
	}
}