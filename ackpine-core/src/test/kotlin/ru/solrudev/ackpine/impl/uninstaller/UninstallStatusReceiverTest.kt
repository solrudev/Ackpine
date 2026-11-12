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
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextWrapper
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.receiver.SystemPackageInstallerStatusReceiver
import ru.solrudev.ackpine.impl.testutil.TestCompletableSession
import ru.solrudev.ackpine.impl.testutil.drainMainThread
import ru.solrudev.ackpine.impl.uninstaller.activity.UninstallActivity
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.VersionedUninstallPolicy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

	@Test
	fun modifyConfirmationWrapperIntentForwardsPackageName() {
		val sourceIntent = Intent().putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, "com.example.app")
		val wrapperIntent = Intent()

		val modifiedIntent = receiver.modifyConfirmationWrapperIntent(sourceIntent, wrapperIntent)

		assertEquals("com.example.app", modifiedIntent.getStringExtra(UninstallActivity.EXTRA_PACKAGE_NAME))
	}

	@Test
	fun strictExactVersionPendingUserActionFailsWithoutConfirmationIntent() {
		val session = sendPendingUserAction(VersionedUninstallPolicy.STRICT, confirmationIntent = null)
		val state = assertIs<Session.State.Failed<UninstallFailure>>(session.completedState)
		assertIs<UninstallFailure.Unsupported>(state.failure)
		assertNull(Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity)
		val receiverShadow = shadowOf(receiver)
		assertTrue(receiverShadow.wentAsync())
		assertTrue(shadowOf(receiverShadow.originalPendingResult).future.isDone)
	}

	@Test
	fun unsafeExactPendingUserActionLaunchesUiAndPreservesConstraints() {
		val session = sendPendingUserAction(VersionedUninstallPolicy.ALLOW_NAME_ONLY)
		assertNull(session.completedState)
		val started = assertNotNull(Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity)
		assertEquals(42L, started.getLongExtra(UninstallActivity.EXTRA_PACKAGE_VERSION_CODE, -1))
		assertEquals(
			VersionedUninstallPolicy.ALLOW_NAME_ONLY.ordinal,
			started.getIntExtra(UninstallActivity.EXTRA_VERSIONED_UNINSTALL_POLICY, -1)
		)
	}

	private fun sendPendingUserAction(
		policy: VersionedUninstallPolicy,
		confirmationIntent: Intent? = Intent(Intent.ACTION_DELETE)
	): TestCompletableSession<UninstallFailure> {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableSession<UninstallFailure>(sessionId)
		PackageUninstallerImpl.getInstance(context).addSession(sessionId, session)
		val action = receiver.getAction(context)
		ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
		try {
			val intent = Intent(action)
				.putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_PENDING_USER_ACTION)
				.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
				.putExtra(SystemPackageInstallerStatusReceiver.EXTRA_CONFIRMATION, Confirmation.IMMEDIATE.ordinal)
				.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, "com.example.app")
				.putExtra(UninstallActivity.EXTRA_PACKAGE_VERSION_CODE, 42L)
				.putExtra(UninstallActivity.EXTRA_VERSIONED_UNINSTALL_POLICY, policy.ordinal)
			SessionIdIntents.putSessionId(intent, sessionId)
			context.sendBroadcast(intent)
			drainMainThread()
		} finally {
			context.unregisterReceiver(receiver)
		}
		return session
	}
}