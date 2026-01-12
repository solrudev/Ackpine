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

package ru.solrudev.ackpine.sample.install

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import ru.solrudev.ackpine.exceptions.NoBaseApkException
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.sample.MainDispatcherRule
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.SplitPackage
import ru.solrudev.ackpine.test.TestInstallSession
import ru.solrudev.ackpine.test.TestPackageInstaller
import ru.solrudev.ackpine.test.TestSessionScript
import ru.solrudev.ackpine.test.futures.ImmediateFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TEST_APK_NAME = "base.apk"

@OptIn(ExperimentalCoroutinesApi::class)
class InstallViewModelTest {

	@JvmField
	@Rule
	val mainDispatcherRule = MainDispatcherRule()

	@Test
	fun installPackageSuccessfulFlow() = runTest(mainDispatcherRule.dispatcher) {
		val installer = TestPackageInstaller(TestSessionScript.empty())
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val viewModel = InstallViewModel(installer, repository)
		viewModel.uiState.test {
			awaitItem() // initial state

			viewModel.installPackage(createSplitPackageProvider(), TEST_APK_NAME)

			advanceUntilIdle()
			val session = installer.sessions.last()

			val expectedState1 = InstallUiState(
				error = ResolvableString.empty(),
				sessions = listOf(
					SessionData(
						session.id,
						name = TEST_APK_NAME,
						error = ResolvableString.empty(),
						isCancellable = true
					)
				),
				sessionsProgress = listOf(
					SessionProgress(session.id, Progress())
				)
			)
			assertEquals(expectedState1, awaitItem())

			val progress = Progress(progress = 50, max = 100)
			session.controller.setProgress(progress)
			val expectedState2 = expectedState1.copy(
				sessionsProgress = listOf(SessionProgress(session.id, progress))
			)
			assertEquals(expectedState2, awaitItem())

			session.controller.setState(Session.State.Committed)
			val expectedState3 = expectedState2.copy(
				sessions = expectedState2.sessions.map { it.copy(isCancellable = false) }
			)
			assertEquals(expectedState3, awaitItem())

			session.controller.succeed()
			assertEquals(InstallUiState(), awaitItem())
		}
	}

	@Test
	fun installPackageFailureFlow() = runTest(mainDispatcherRule.dispatcher) {
		val script: TestSessionScript<InstallFailure> = TestSessionScript.auto(
			Session.State.Failed(InstallFailure.Generic("Failure"))
		)
		val installer = TestPackageInstaller(script)
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val viewModel = InstallViewModel(installer, repository)
		viewModel.uiState.test {
			awaitItem() // initial state

			viewModel.installPackage(createSplitPackageProvider(), TEST_APK_NAME)
			awaitItem() // session created
			val session = installer.sessions.last()

			val expectedState = InstallUiState(
				error = ResolvableString.empty(),
				sessions = listOf(
					SessionData(
						session.id,
						name = TEST_APK_NAME,
						error = ResolvableString.transientResource(R.string.session_error_with_reason, "Failure"),
						isCancellable = true
					)
				),
				sessionsProgress = listOf(
					SessionProgress(session.id, Progress())
				)
			)
			assertEquals(expectedState, awaitItem())

			viewModel.removeSession(session.id)
			assertEquals(InstallUiState(), awaitItem())
		}
	}

	@Test
	fun cancelSessionRemovesSession() = runTest(mainDispatcherRule.dispatcher) {
		val installer = TestPackageInstaller(TestSessionScript.empty())
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val viewModel = InstallViewModel(installer, repository)
		viewModel.uiState.test {
			awaitItem() // initial state

			viewModel.installPackage(createSplitPackageProvider(), TEST_APK_NAME)

			val sessionCreatedState = awaitItem()
			assertTrue(sessionCreatedState.error.isEmpty)
			assertTrue(sessionCreatedState.sessions.isNotEmpty())
			assertTrue(sessionCreatedState.sessionsProgress.isNotEmpty())

			val session = installer.sessions.last()
			viewModel.cancelSession(session.id)

			assertEquals(InstallUiState(), awaitItem())
		}
	}

	@Test
	fun installPackageReportsSplitPackageErrors() = runTest(mainDispatcherRule.dispatcher) {
		val installer = TestPackageInstaller()
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val viewModel = InstallViewModel(installer, repository)
		viewModel.uiState.test {
			awaitItem() // initial state

			val provider = createFailingSplitPackageProvider(NoBaseApkException())
			viewModel.installPackage(provider, "broken.apk")
			val expectedState = InstallUiState(
				error = ResolvableString.transientResource(R.string.error_no_base_apk)
			)
			assertEquals(expectedState, awaitItem())

			viewModel.clearError()
			assertEquals(InstallUiState(), awaitItem())
		}
	}

	@Test
	fun restoresSessionsFromSavedStateAndClearsOnCompletion() = runTest(mainDispatcherRule.dispatcher) {
		val installer = TestPackageInstaller()
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val sessions = listOf(
			TestInstallSession(TestSessionScript.empty()),
			TestInstallSession(TestSessionScript.empty())
		).onEach(installer::seedSession)
		val sessionData = sessions
			.map { session -> SessionData(session.id, TEST_APK_NAME) }
			.onEach(repository::addSessionData)
		val sessionProgress = sessions.map { session -> SessionProgress(session.id, Progress()) }
		val viewModel = InstallViewModel(installer, repository)
		viewModel.uiState.test {
			awaitItem() // initial state

			val expectedState = InstallUiState(
				sessions = sessionData,
				sessionsProgress = sessionProgress
			)
			assertEquals(expectedState, awaitItem())

			for (session in sessions) {
				session.controller.succeed()
			}
			assertEquals(InstallUiState(), awaitItem())
		}
	}

	private fun createSplitPackageProvider(): SplitPackage.Provider {
		val apk = Apk.Base(
			uri = Uri.EMPTY,
			name = "base",
			size = 1024,
			packageName = "com.example",
			versionCode = 1,
			versionName = "1.0"
		)
		val splitPackage = SplitPackage(
			base = listOf(SplitPackage.Entry(isPreferred = true, apk)),
			libs = emptyList(),
			screenDensity = emptyList(),
			localization = emptyList(),
			other = emptyList(),
			dynamicFeatures = emptyList()
		)
		return SplitPackage.Provider { ImmediateFuture.success(splitPackage) }
	}

	private fun createFailingSplitPackageProvider(exception: Exception): SplitPackage.Provider {
		return SplitPackage.Provider { ImmediateFuture.failure(exception) }
	}
}