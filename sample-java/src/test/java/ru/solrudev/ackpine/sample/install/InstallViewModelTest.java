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

package ru.solrudev.ackpine.sample.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static java.util.Collections.emptyList;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import ru.solrudev.ackpine.exceptions.NoBaseApkException;
import ru.solrudev.ackpine.installer.InstallFailure;
import ru.solrudev.ackpine.resources.ResolvableString;
import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.session.Progress;
import ru.solrudev.ackpine.session.Session;
import ru.solrudev.ackpine.splits.Apk;
import ru.solrudev.ackpine.splits.SplitPackage;
import ru.solrudev.ackpine.test.TestPackageInstaller;
import ru.solrudev.ackpine.test.TestProgressSession;
import ru.solrudev.ackpine.test.TestSessionScript;
import ru.solrudev.ackpine.test.futures.ImmediateFuture;

public class InstallViewModelTest {

	@Rule
	public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

	private static final String TEST_APK_NAME = "base.apk";

	@Test
	public void installPackageSuccessfulFlow() {
		final var installer = new TestPackageInstaller(TestSessionScript.empty());
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var viewModel = new InstallViewModel(installer, repository);

		viewModel.installPackage(createSplitPackageProvider(), TEST_APK_NAME);

		final var session = installer.getSessions().getLast();
		final var expectedSessions1 = List.of(new SessionData(session.getId(), TEST_APK_NAME));
		final var expectedProgress1 = List.of(new SessionProgress(session.getId(), new Progress()));
		assertEquals(expectedSessions1, viewModel.getSessions().getValue());
		assertEquals(expectedProgress1, viewModel.getSessionsProgress().getValue());

		final var sessionController = session.getController();
		final var progress = new Progress(50, 100);

		sessionController.setProgress(progress);
		final var expectedProgress2 = List.of(new SessionProgress(session.getId(), progress));
		assertEquals(expectedProgress2, viewModel.getSessionsProgress().getValue());

		sessionController.setState(Session.State.Committed.INSTANCE);
		final var expectedSessions2 = List.of(new SessionData(session.getId(), TEST_APK_NAME,
				ResolvableString.empty(), false));
		assertEquals(expectedSessions2, viewModel.getSessions().getValue());

		sessionController.succeed();
		assertTrue(viewModel.getError().getValue().isEmpty());
		assertTrue(viewModel.getSessions().getValue().isEmpty());
		assertTrue(viewModel.getSessionsProgress().getValue().isEmpty());
	}

	@Test
	public void installPackageFailureFlow() {
		final var failure = new Session.State.Failed<>(new InstallFailure.Generic("Failure"));
		TestSessionScript<InstallFailure> script = TestSessionScript.auto(failure);
		final var installer = new TestPackageInstaller(script);
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var viewModel = new InstallViewModel(installer, repository);

		viewModel.installPackage(createSplitPackageProvider(), TEST_APK_NAME);

		final var session = installer.getSessions().getLast();
		final var expectedError = ResolvableString.transientResource(R.string.session_error_with_reason, "Failure");
		final var expectedSession = new SessionData(session.getId(), TEST_APK_NAME, expectedError, true);
		final var expectedProgress = new SessionProgress(session.getId(), new Progress());
		assertTrue(viewModel.getError().getValue().isEmpty());
		assertEquals(List.of(expectedSession), viewModel.getSessions().getValue());
		assertEquals(List.of(expectedProgress), viewModel.getSessionsProgress().getValue());

		viewModel.removeSession(session.getId());

		assertTrue(viewModel.getError().getValue().isEmpty());
		assertTrue(viewModel.getSessions().getValue().isEmpty());
		assertTrue(viewModel.getSessionsProgress().getValue().isEmpty());
	}

	@Test
	public void cancelSessionRemovesSession() {
		final var installer = new TestPackageInstaller(TestSessionScript.empty());
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var viewModel = new InstallViewModel(installer, repository);

		viewModel.installPackage(createSplitPackageProvider(), TEST_APK_NAME);

		assertTrue(viewModel.getError().getValue().isEmpty());
		assertFalse(viewModel.getSessions().getValue().isEmpty());
		assertFalse(viewModel.getSessionsProgress().getValue().isEmpty());

		final var session = installer.getSessions().getLast();
		viewModel.cancelSession(session.getId());

		assertTrue(viewModel.getError().getValue().isEmpty());
		assertTrue(viewModel.getSessions().getValue().isEmpty());
		assertTrue(viewModel.getSessionsProgress().getValue().isEmpty());
	}

	@Test
	public void installPackageReportsSplitPackageErrors() {
		final var installer = new TestPackageInstaller();
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var viewModel = new InstallViewModel(installer, repository);

		viewModel.installPackage(createFailingSplitPackageProvider(new NoBaseApkException()), "broken.apk");

		assertTrue(repository.getSessions().getValue().isEmpty());
		final var expectedError = ResolvableString.transientResource(R.string.error_no_base_apk);
		assertEquals(expectedError, viewModel.getError().getValue());

		viewModel.clearError();
		assertTrue(viewModel.getError().getValue().isEmpty());
	}

	@Test
	public void restoresSessionsFromSavedStateAndClearsOnCompletion() {
		final var installer = new TestPackageInstaller();
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var sessions = List.of(
				new TestProgressSession<InstallFailure>(TestSessionScript.empty()),
				new TestProgressSession<InstallFailure>(TestSessionScript.empty())
		);
		for (final var session : sessions) {
			repository.addSessionData(new SessionData(session.getId(), TEST_APK_NAME));
			installer.seedSession(session);
		}

		final var expectedSessions = List.of(
				new SessionData(sessions.getFirst().getId(), TEST_APK_NAME),
				new SessionData(sessions.getLast().getId(), TEST_APK_NAME)
		);
		final var expectedProgress = List.of(
				new SessionProgress(sessions.getFirst().getId(), new Progress()),
				new SessionProgress(sessions.getLast().getId(), new Progress())
		);

		final var viewModel = new InstallViewModel(installer, repository);
		assertTrue(viewModel.getError().getValue().isEmpty());
		assertEquals(expectedSessions, viewModel.getSessions().getValue());
		assertEquals(expectedProgress, viewModel.getSessionsProgress().getValue());

		for (final var session : sessions) {
			session.getController().succeed();
		}
		assertTrue(viewModel.getError().getValue().isEmpty());
		assertTrue(viewModel.getSessions().getValue().isEmpty());
		assertTrue(viewModel.getSessionsProgress().getValue().isEmpty());
	}

	private static @NonNull SplitPackage.Provider createSplitPackageProvider() {
		final var apk = new Apk.Base(Uri.EMPTY, "base", 1024L, "com.example", 1L, "1.0");
		final var splitPackage = new SplitPackage(
				List.of(new SplitPackage.Entry<>(true, apk)),
				emptyList(),
				emptyList(),
				emptyList(),
				emptyList(),
				emptyList()
		);
		return () -> ImmediateFuture.success(splitPackage);
	}

	private static @NonNull SplitPackage.Provider createFailingSplitPackageProvider(Exception exception) {
		return () -> ImmediateFuture.failure(exception);
	}
}