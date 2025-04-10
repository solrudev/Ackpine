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

package ru.solrudev.ackpine.sample.uninstall;

import static androidx.lifecycle.SavedStateHandleSupport.createSavedStateHandle;
import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Supplier;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.solrudev.ackpine.DisposableSubscriptionContainer;
import ru.solrudev.ackpine.session.Failure;
import ru.solrudev.ackpine.session.Session;
import ru.solrudev.ackpine.session.parameters.Confirmation;
import ru.solrudev.ackpine.uninstaller.PackageUninstaller;
import ru.solrudev.ackpine.uninstaller.UninstallFailure;
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters;

public final class UninstallViewModel extends ViewModel {

	private static final String SESSION_ID_KEY = "SESSION_ID";
	private static final String PACKAGE_NAME_KEY = "PACKAGE_NAME";
	private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
	private final MutableLiveData<List<ApplicationData>> applications = new MutableLiveData<>(new ArrayList<>());
	private final DisposableSubscriptionContainer subscriptions = new DisposableSubscriptionContainer();
	private final PackageUninstaller packageUninstaller;
	private final SavedStateHandle savedStateHandle;
	private final ExecutorService executor;

	public UninstallViewModel(@NonNull PackageUninstaller packageUninstaller,
							  @NonNull SavedStateHandle savedStateHandle,
							  @NonNull ExecutorService executor) {
		this.packageUninstaller = packageUninstaller;
		this.savedStateHandle = savedStateHandle;
		this.executor = executor;
		final var sessionId = getSessionId();
		if (sessionId != null) {
			addSessionListener(sessionId);
		}
	}

	@Override
	protected void onCleared() {
		subscriptions.clear();
		executor.shutdownNow();
		final var sessionId = getSessionId();
		if (sessionId != null) {
			cancelSession(sessionId);
			clearSavedState();
		}
	}

	@NonNull
	public LiveData<Boolean> isLoading() {
		return isLoading;
	}

	@NonNull
	public LiveData<List<ApplicationData>> getApplications() {
		return applications;
	}

	public void loadApplications(boolean refresh, @NonNull Supplier<List<ApplicationData>> applicationsFactory) {
		if (!refresh && !getCurrentApplications().isEmpty()) {
			return;
		}
		executor.execute(() -> {
			isLoading.postValue(true);
			final var applications = applicationsFactory.get();
			this.applications.postValue(applications);
			isLoading.postValue(false);
		});
	}

	public void uninstallPackage(@NonNull String packageName) {
		final var session =
				packageUninstaller.createSession(new UninstallParameters.Builder(packageName)
						.setConfirmation(Confirmation.IMMEDIATE)
						.build());
		savedStateHandle.set(SESSION_ID_KEY, session.getId());
		savedStateHandle.set(PACKAGE_NAME_KEY, packageName);
		attachSessionStateListener(session);
	}

	private UUID getSessionId() {
		return savedStateHandle.get(SESSION_ID_KEY);
	}

	private void cancelSession(@NonNull UUID id) {
		final var future = packageUninstaller.getSessionAsync(id);
		Futures.addCallback(future, new FutureCallback<Session<UninstallFailure>>() {
			@Override
			public void onSuccess(@Nullable Session<UninstallFailure> session) {
				if (session != null) {
					session.cancel();
				}
			}

			@Override
			public void onFailure(@NonNull Throwable t) { // no-op
			}
		}, MoreExecutors.directExecutor());
	}

	private void addSessionListener(@NonNull UUID id) {
		final var future = packageUninstaller.getSessionAsync(id);
		Futures.addCallback(future, new FutureCallback<Session<UninstallFailure>>() {
			@Override
			public void onSuccess(@Nullable Session<UninstallFailure> session) {
				if (session != null) {
					attachSessionStateListener(session);
				}
			}

			@Override
			public void onFailure(@NonNull Throwable t) { // no-op
			}
		}, MoreExecutors.directExecutor());
	}

	private void attachSessionStateListener(@NonNull Session<UninstallFailure> session) {
		Session.TerminalStateListener.bind(session, subscriptions)
				.addOnCancelListener(sessionId -> clearSavedState())
				.addOnSuccessListener(sessionId -> {
					final String packageName = savedStateHandle.get(PACKAGE_NAME_KEY);
					if (packageName != null) {
						removeApplication(packageName);
					}
					clearSavedState();
				})
				.addOnFailureListener((sessionId, failure) -> {
					clearSavedState();
					if (failure instanceof Failure.Exceptional f) {
						Log.e("UninstallViewModel", null, f.getException());
					}
				});
	}

	@NonNull
	private List<ApplicationData> getCurrentApplications() {
		return new ArrayList<>(Objects.requireNonNull(this.applications.getValue()));
	}

	private void clearSavedState() {
		savedStateHandle.remove(SESSION_ID_KEY);
		savedStateHandle.remove(PACKAGE_NAME_KEY);
	}

	private void removeApplication(@NonNull String packageName) {
		final var currentApplications = getCurrentApplications();
		final var applicationDataIndex = getApplicationIndexByPackageName(currentApplications, packageName);
		if (applicationDataIndex != -1) {
			currentApplications.remove(applicationDataIndex);
		}
		applications.setValue(currentApplications);
	}

	private static int getApplicationIndexByPackageName(@NonNull List<ApplicationData> applications,
														@NonNull String packageName) {
		for (var i = 0; i < applications.size(); i++) {
			final var applicationData = applications.get(i);
			if (applicationData.packageName().equals(packageName)) {
				return i;
			}
		}
		return -1;
	}

	static final ViewModelInitializer<UninstallViewModel> initializer = new ViewModelInitializer<>(
			UninstallViewModel.class,
			creationExtras -> {
				final var application = creationExtras.get(APPLICATION_KEY);
				assert application != null;
				final var packageUninstaller = PackageUninstaller.getInstance(application);
				final var savedStateHandle = createSavedStateHandle(creationExtras);
				final var executor = Executors.newFixedThreadPool(8);
				return new UninstallViewModel(packageUninstaller, savedStateHandle, executor);
			}
	);
}