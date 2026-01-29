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

package ru.solrudev.ackpine.impl.installer.receiver

import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.installer.InstallFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PackageInstallerStatusReceiverTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val receiver = PackageInstallerStatusReceiver()

	@Test
	fun getFailureReturnsGenericForStatusFailure() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE,
			message = "Something went wrong",
			otherPackageName = null,
			storagePath = null
		)
		assertIs<InstallFailure.Generic>(failure)
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
		assertIs<InstallFailure.Aborted>(failure)
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
		assertIs<InstallFailure.Blocked>(failure)
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
		assertIs<InstallFailure.Conflict>(failure)
		assertEquals("Package conflict", failure.message)
		assertEquals("com.example.other", failure.otherPackageName)
	}

	@Test
	fun getFailureReturnsIncompatibleForStatusFailureIncompatible() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
			message = "Incompatible with device",
			otherPackageName = null,
			storagePath = null
		)
		assertIs<InstallFailure.Incompatible>(failure)
		assertEquals("Incompatible with device", failure.message)
	}

	@Test
	fun getFailureReturnsInvalidForStatusFailureInvalid() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE_INVALID,
			message = "APK is invalid",
			otherPackageName = null,
			storagePath = null
		)
		assertIs<InstallFailure.Invalid>(failure)
		assertEquals("APK is invalid", failure.message)
	}

	@Test
	fun getFailureReturnsStorageForStatusFailureStorage() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE_STORAGE,
			message = "Insufficient storage",
			otherPackageName = null,
			storagePath = "/sdcard"
		)
		assertIs<InstallFailure.Storage>(failure)
		assertEquals("Insufficient storage", failure.message)
		assertEquals("/sdcard", failure.storagePath)
	}

	@Test
	fun getFailureReturnsTimeoutForStatusFailureTimeout() {
		val failure = receiver.getFailure(
			status = PackageInstaller.STATUS_FAILURE_TIMEOUT,
			message = "Installation timed out",
			otherPackageName = null,
			storagePath = null
		)
		assertIs<InstallFailure.Timeout>(failure)
		assertEquals("Installation timed out", failure.message)
	}

	@Test
	fun getFailureReturnsGenericForUnknownStatus() {
		val failure = receiver.getFailure(
			status = 999,
			message = "Unknown status",
			otherPackageName = null,
			storagePath = null
		)
		assertIs<InstallFailure.Generic>(failure)
		assertEquals("Unknown failure", failure.message)
	}

	@Test
	fun getActionReturnsPackageInstallerStatusAction() {
		val action = receiver.getAction(context)
		assertTrue(action.endsWith(".PACKAGE_INSTALLER_STATUS"))
		assertTrue(action.startsWith(context.packageName))
	}

	@Test
	fun getActionFromCompanionMatchesInstanceMethod() {
		val actionFromInstance = receiver.getAction(context)
		val actionFromCompanion = PackageInstallerStatusReceiver.getAction(context)
		assertEquals(actionFromCompanion, actionFromInstance)
	}
}