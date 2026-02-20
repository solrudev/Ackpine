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

package ru.solrudev.ackpine.impl.uninstaller

import android.content.Context
import android.content.pm.PackageInstaller
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class UninstallStatusReceiverTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val receiver = UninstallStatusReceiver()

	@Test
	fun getFailureReturnsGenericForStatusFailure() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE,
			message = "Something went wrong",
			otherPackageName = null,
			storagePath = null
		)
		assertIs<UninstallFailure.Generic>(failure)
		assertEquals("Something went wrong", failure.message)
	}

	@Test
	fun getFailureReturnsAbortedForStatusFailureAborted() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE_ABORTED,
			message = "User cancelled",
			otherPackageName = null,
			storagePath = null
		)
		assertIs<UninstallFailure.Aborted>(failure)
		assertEquals("User cancelled", failure.message)
	}

	@Test
	fun getFailureReturnsBlockedForStatusFailureBlocked() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE_BLOCKED,
			message = "Blocked by MDM",
			otherPackageName = "com.example.blocker",
			storagePath = null
		)
		assertIs<UninstallFailure.Blocked>(failure)
		assertEquals("Blocked by MDM", failure.message)
		assertEquals("com.example.blocker", failure.otherPackageName)
	}

	@Test
	fun getFailureReturnsConflictForStatusFailureConflict() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE_CONFLICT,
			message = "Package conflict",
			otherPackageName = "com.example.other",
			storagePath = null
		)
		assertIs<UninstallFailure.Conflict>(failure)
		assertEquals("Package conflict", failure.message)
		assertEquals("com.example.other", failure.otherPackageName)
	}

	@Test
	fun getFailureReturnsGenericForUnknownStatus() {
		val failure = receiver.getFailure(
			status = 999,
			message = "Unknown status",
			otherPackageName = null,
			storagePath = null
		)
		assertIs<UninstallFailure.Generic>(failure)
		assertEquals("Unknown failure", failure.message)
	}

	@Test
	fun getActionReturnsPackageUninstallerStatusAction() {
		val action = receiver.getAction(context)
		assertTrue(action.endsWith(".PACKAGE_UNINSTALLER_STATUS"))
		assertTrue(action.startsWith(context.packageName))
	}

	@Test
	fun getActionFromCompanionMatchesInstanceMethod() {
		val actionFromInstance = receiver.getAction(context)
		val actionFromCompanion = UninstallStatusReceiver.getAction(context)
		assertEquals(actionFromCompanion, actionFromInstance)
	}
}