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

package ru.solrudev.ackpine.sample.uninstall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import ru.solrudev.ackpine.session.Session;
import ru.solrudev.ackpine.test.TestSession;
import ru.solrudev.ackpine.test.TestSessionScript;
import ru.solrudev.ackpine.test.TestPackageUninstaller;
import ru.solrudev.ackpine.uninstaller.UninstallFailure;

public class UninstallViewModelTest {

	@Rule
	public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

	private final ExecutorService directExecutor = MoreExecutors.newDirectExecutorService();
	private static final String SESSION_ID_KEY = "SESSION_ID";
	private static final String PACKAGE_NAME_KEY = "PACKAGE_NAME";
	private static final String PACKAGE_NAME = "com.example.app";

	@Test
	public void loadApplicationsPopulatesUiState() {
		final var viewModel = new UninstallViewModel(new TestPackageUninstaller(),
				new SavedStateHandle(),
				directExecutor);
		final var app = createApplicationData();

		final var loadingValues = new ArrayList<Boolean>();
		viewModel.isLoading().observeForever(loadingValues::add);

		viewModel.loadApplications(true, () -> List.of(app));

		assertEquals(List.of(false, true, false), loadingValues);
		assertEquals(List.of(app), viewModel.getApplications().getValue());
	}

	@Test
	public void uninstallPackageSuccessfulFlow() {
		final var uninstaller = new TestPackageUninstaller();
		final var savedStateHandle = new SavedStateHandle();

		final var sessionIds = new ArrayList<UUID>();
		savedStateHandle.<UUID>getLiveData(SESSION_ID_KEY, null).observeForever(sessionIds::add);
		final var packageNames = new ArrayList<String>();
		savedStateHandle.<String>getLiveData(PACKAGE_NAME_KEY, null).observeForever(packageNames::add);

		final var viewModel = new UninstallViewModel(uninstaller, savedStateHandle, directExecutor);
		final var app = createApplicationData();
		viewModel.loadApplications(true, () -> List.of(app));

		viewModel.uninstallPackage(app.packageName());
		sessionIds.add(savedStateHandle.get(SESSION_ID_KEY));
		packageNames.add(savedStateHandle.get(PACKAGE_NAME_KEY));

		final var session = uninstaller.getSessions().getLast();
		session.getController().succeed();

		assertTrue(viewModel.getApplications().getValue().isEmpty());
		assertNull(viewModel.getFailure().getValue());
		assertEquals(Arrays.asList(null, session.getId(), null), sessionIds);
		assertEquals(Arrays.asList(null, app.packageName(), null), packageNames);
	}

	@Test
	public void uninstallPackageFailureFlow() {
		final var failure = new Session.State.Failed<>(new UninstallFailure.Generic("Failure"));
		TestSessionScript<UninstallFailure> script = TestSessionScript.auto(failure);
		final var uninstaller = new TestPackageUninstaller(script);
		final var savedStateHandle = new SavedStateHandle();
		final var viewModel = new UninstallViewModel(uninstaller, savedStateHandle, directExecutor);
		final var app = createApplicationData();

		viewModel.loadApplications(true, () -> List.of(app));
		viewModel.uninstallPackage(app.packageName());
		uninstaller.getSessions().getLast().getController().fail(new UninstallFailure.Generic("Failure"));

		assertEquals("Failure", viewModel.getFailure().getValue());
		assertEquals(List.of(app), viewModel.getApplications().getValue());
		assertNull(savedStateHandle.get(SESSION_ID_KEY));
		assertNull(savedStateHandle.get(PACKAGE_NAME_KEY));

		viewModel.clearFailure();
		assertNull(viewModel.getFailure().getValue());
	}

	@Test
	public void restoresSessionFromSavedStateAndClearsOnCompletion() {
		final var session = new TestSession<UninstallFailure>();
		final var uninstaller = new TestPackageUninstaller();
		uninstaller.seedSession(session);
		final var savedStateHandle = new SavedStateHandle(
				Map.of(
						SESSION_ID_KEY, session.getId(),
						PACKAGE_NAME_KEY, PACKAGE_NAME
				)
		);

		new UninstallViewModel(uninstaller, savedStateHandle, directExecutor);

		assertNull(savedStateHandle.get(SESSION_ID_KEY));
		assertNull(savedStateHandle.get(PACKAGE_NAME_KEY));
	}

	@NonNull
	private static ApplicationData createApplicationData() {
		return new ApplicationData("App", PACKAGE_NAME, new TestDrawable());
	}
}