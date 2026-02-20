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

package ru.solrudev.ackpine.sample.updater

import android.content.ContentProvider
import android.content.Context
import android.content.pm.ProviderInfo
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.AssetFileProvider
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.test.TestInstallSession
import ru.solrudev.ackpine.test.TestPackageInstaller
import ru.solrudev.ackpine.test.TestSessionScript
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val SESSION_ID_KEY = "SESSION_ID"
private const val OKHTTP_PROVIDER_AUTHORITY = "ru.solrudev.ackpine.sample.updater.OkHttpProvider"
private const val ASSET_FILE_PROVIDER_AUTHORITY = "ru.solrudev.ackpine.sample.updater.AssetFileProvider"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

	@JvmField
	@Rule
	val mainDispatcherRule = MainDispatcherRule()

	private val context: Context = ApplicationProvider.getApplicationContext()

	init {
		attachProvider(OkHttpProvider(), OKHTTP_PROVIDER_AUTHORITY)
		attachProvider(AssetFileProvider(), ASSET_FILE_PROVIDER_AUTHORITY)
	}

	@Test
	fun installSuccessfulFlow() = runTest(mainDispatcherRule.dispatcher) {
		val installer = TestPackageInstaller(TestSessionScript.empty())
		val savedStateHandle = SavedStateHandle()
		val viewModel = MainViewModel(installer, savedStateHandle)
		viewModel.uiState.test {
			awaitItem() // initial state

			viewModel.onButtonClick()
			advanceUntilIdle()

			val session = installer.sessions.single()
			assertEquals(session.id, savedStateHandle[SESSION_ID_KEY])
			val installingState = UpdaterUiState(
				isInstalling = true,
				buttonText = ResolvableString.transientResource(R.string.cancel)
			)
			assertEquals(installingState, awaitItem())

			session.controller.setState(Session.State.Committed)
			val committedState = installingState.copy(isCancellable = false)
			assertEquals(committedState, awaitItem())

			session.controller.succeed()
			assertEquals(UpdaterUiState(), awaitItem())
			assertNull(savedStateHandle[SESSION_ID_KEY])
		}
	}

	@Test
	fun sessionFailureWithMessageSetsErrorWithReason() = testInstallFailure(
		failure = InstallFailure.Generic("Failure"),
		expectedError = ResolvableString.transientResource(R.string.error_with_reason, "Failure")
	)

	@Test
	fun sessionFailureWithoutMessageSetsGenericError() = testInstallFailure(
		failure = InstallFailure.Generic(),
		expectedError = ResolvableString.transientResource(R.string.error)
	)

	private fun testInstallFailure(
		failure: InstallFailure,
		expectedError: ResolvableString
	) = runTest(mainDispatcherRule.dispatcher) {
		val script = TestSessionScript.auto(Session.State.Failed(failure))
		val installer = TestPackageInstaller(script)
		val savedStateHandle = SavedStateHandle()
		val viewModel = MainViewModel(installer, savedStateHandle)
		viewModel.uiState.test {
			awaitItem() // initial state

			viewModel.onButtonClick()
			advanceUntilIdle()

			assertEquals(UpdaterUiState(error = expectedError), awaitItem())
			advanceUntilIdle()
			assertNull(savedStateHandle[SESSION_ID_KEY])
		}
	}

	@Test
	fun buttonClickWhileInstallingCancelsSession() = runTest(mainDispatcherRule.dispatcher) {
		val installer = TestPackageInstaller(TestSessionScript.empty())
		val savedStateHandle = SavedStateHandle()
		val viewModel = MainViewModel(installer, savedStateHandle)
		viewModel.uiState.test {
			awaitItem() // initial state

			viewModel.onButtonClick()
			advanceUntilIdle()
			awaitItem() // installing

			viewModel.onButtonClick()
			advanceUntilIdle()
			assertEquals(UpdaterUiState(), awaitItem())
			assertNull(savedStateHandle[SESSION_ID_KEY])
		}
	}

	@Test
	fun restoresSessionFromSavedStateAndResumesSeededSession() = runTest(mainDispatcherRule.dispatcher) {
		val sessionId = UUID.randomUUID()
		val session = TestInstallSession(TestSessionScript.empty(), sessionId)
		val installer = TestPackageInstaller().apply {
			seedSession(session)
		}
		val savedStateHandle = SavedStateHandle(mapOf(SESSION_ID_KEY to sessionId))
		val viewModel = MainViewModel(installer, savedStateHandle)
		viewModel.uiState.test {
			awaitItem() // initial state
			advanceUntilIdle()
			awaitItem() // installing

			session.controller.succeed()
			assertEquals(UpdaterUiState(), awaitItem())

			advanceUntilIdle()
			assertNull(savedStateHandle[SESSION_ID_KEY])
		}
	}

	private fun attachProvider(provider: ContentProvider, authority: String) {
		provider.attachInfo(
			context,
			ProviderInfo().apply {
				this.authority = authority
			}
		)
	}
}