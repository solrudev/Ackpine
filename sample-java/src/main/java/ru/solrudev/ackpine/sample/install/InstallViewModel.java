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

package ru.solrudev.ackpine.sample.install;

import static androidx.lifecycle.SavedStateHandleSupport.createSavedStateHandle;
import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;

import ru.solrudev.ackpine.DisposableSubscriptionContainer;
import ru.solrudev.ackpine.exceptions.ConflictingBaseApkException;
import ru.solrudev.ackpine.exceptions.ConflictingPackageNameException;
import ru.solrudev.ackpine.exceptions.ConflictingSplitNameException;
import ru.solrudev.ackpine.exceptions.ConflictingVersionCodeException;
import ru.solrudev.ackpine.exceptions.NoBaseApkException;
import ru.solrudev.ackpine.exceptions.SplitPackageException;
import ru.solrudev.ackpine.installer.InstallFailure;
import ru.solrudev.ackpine.installer.PackageInstaller;
import ru.solrudev.ackpine.installer.parameters.InstallParameters;
import ru.solrudev.ackpine.resources.ResolvableString;
import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.session.Failure;
import ru.solrudev.ackpine.session.ProgressSession;
import ru.solrudev.ackpine.session.Session;
import ru.solrudev.ackpine.splits.SplitPackage;

public final class InstallViewModel extends ViewModel {

	private final MutableLiveData<ResolvableString> error = new MutableLiveData<>();
	private final DisposableSubscriptionContainer subscriptions = new DisposableSubscriptionContainer();
	private final PackageInstaller packageInstaller;
	private final SessionDataRepository sessionDataRepository;
	private final List<ListenableFuture<?>> futures = new ArrayList<>();

	public InstallViewModel(@NonNull PackageInstaller packageInstaller,
							@NonNull SessionDataRepository sessionDataRepository) {
		this.packageInstaller = packageInstaller;
		this.sessionDataRepository = sessionDataRepository;
		final var sessions = getSessionsSnapshot();
		if (sessions != null && !sessions.isEmpty()) {
			for (final var sessionData : sessions) {
				addSessionListeners(sessionData.id());
			}
		}
	}

	public void installPackage(@NonNull SplitPackage.Provider splitPackageProvider, @NonNull String fileName) {
		final var splitPackageFuture = splitPackageProvider.getAsync();
		futures.add(splitPackageFuture);
		Futures.addCallback(splitPackageFuture, new FutureCallback<>() {
			@Override
			public void onSuccess(SplitPackage splitPackage) {
				installPackage(splitPackage, fileName);
			}

			@Override
			public void onFailure(@NonNull Throwable exception) {
				if (exception instanceof CancellationException) {
					return;
				}
				if (exception instanceof SplitPackageException e) {
					handleSplitPackageException(e);
					return;
				}
				final var message = exception.getMessage() != null ? exception.getMessage() : "";
				error.postValue(ResolvableString.raw(message));
				Log.e("InstallViewModel", null, exception);
			}
		}, MoreExecutors.directExecutor());
	}

	@NonNull
	public LiveData<ResolvableString> getError() {
		return error;
	}

	@NonNull
	public LiveData<List<SessionData>> getSessions() {
		return sessionDataRepository.getSessions();
	}

	@NonNull
	public LiveData<List<SessionProgress>> getSessionsProgress() {
		return sessionDataRepository.getSessionsProgress();
	}

	public void clearError() {
		error.setValue(ResolvableString.empty());
	}

	public void cancelSession(@NonNull UUID id) {
		Futures.addCallback(packageInstaller.getSessionAsync(id), new FutureCallback<>() {
			@Override
			public void onSuccess(@Nullable ProgressSession<InstallFailure> session) {
				if (session != null) {
					session.cancel();
				}
			}

			@Override
			public void onFailure(@NonNull Throwable t) { // no-op
			}
		}, MoreExecutors.directExecutor());
	}

	public void removeSession(@NonNull UUID id) {
		sessionDataRepository.removeSessionData(id);
	}

	@Override
	protected void onCleared() {
		subscriptions.clear();
		for (final var future : futures) {
			future.cancel(false);
		}
		futures.clear();
		final var sessions = getSessionsSnapshot();
		if (sessions != null && !sessions.isEmpty()) {
			for (final var sessionData : sessions) {
				cancelSession(sessionData.id());
				sessionDataRepository.removeSessionData(sessionData.id());
			}
		}
	}

	private void installPackage(@NonNull SplitPackage splitPackage, @NonNull String fileName) {
		final var apks = splitPackage.toList();
		if (apks.isEmpty()) {
			return;
		}
		final var uris = new ArrayList<Uri>();
		for (final var entry : apks) {
			uris.add(entry.getApk().getUri());
		}
		final var session = packageInstaller.createSession(new InstallParameters.Builder(uris)
				.setName(fileName)
				.build());
		final var sessionData = new SessionData(session.getId(), fileName);
		sessionDataRepository.addSessionData(sessionData);
		addSessionListeners(session);
	}

	private void handleSplitPackageException(SplitPackageException exception) {
		if (exception instanceof NoBaseApkException) {
			error.postValue(ResolvableString.transientResource(R.string.error_no_base_apk));
		} else if (exception instanceof ConflictingBaseApkException) {
			error.postValue(ResolvableString.transientResource(R.string.error_conflicting_base_apk));
		} else if (exception instanceof ConflictingSplitNameException e) {
			error.postValue(ResolvableString.transientResource(R.string.error_conflicting_split_name, e.getName()));
		} else if (exception instanceof ConflictingPackageNameException e) {
			error.postValue(ResolvableString.transientResource(R.string.error_conflicting_package_name,
					e.getExpected(), e.getActual(), e.getName()));
		} else if (exception instanceof ConflictingVersionCodeException e) {
			error.postValue(ResolvableString.transientResource(R.string.error_conflicting_version_code,
					e.getExpected(), e.getActual(), e.getName()));
		}
	}

	private void addSessionListeners(@NonNull UUID id) {
		Futures.addCallback(packageInstaller.getSessionAsync(id), new FutureCallback<>() {
			@Override
			public void onSuccess(@Nullable ProgressSession<InstallFailure> session) {
				if (session != null) {
					addSessionListeners(session);
				}
			}

			@Override
			public void onFailure(@NonNull Throwable t) { // no-op
			}
		}, MoreExecutors.directExecutor());
	}

	private void addSessionListeners(@NonNull ProgressSession<InstallFailure> session) {
		session.addStateListener(subscriptions, new SessionStateListener(session));
		session.addProgressListener(subscriptions, sessionDataRepository::updateSessionProgress);
		session.addStateListener(subscriptions, (sessionId, state) -> {
			if (state instanceof Session.State.Committed) {
				sessionDataRepository.updateSessionIsCancellable(sessionId, false);
			}
		});
	}

	private List<SessionData> getSessionsSnapshot() {
		return sessionDataRepository.getSessions().getValue();
	}

	private final class SessionStateListener extends Session.TerminalStateListener<InstallFailure> {

		public SessionStateListener(@NonNull Session<? extends InstallFailure> session) {
			super(session);
		}

		@Override
		public void onCancelled(@NonNull UUID sessionId) {
			sessionDataRepository.removeSessionData(sessionId);
		}

		@Override
		public void onSuccess(@NonNull UUID sessionId) {
			sessionDataRepository.removeSessionData(sessionId);
		}

		@Override
		public void onFailure(@NonNull UUID sessionId, @NonNull InstallFailure failure) {
			final var message = failure.getMessage();
			final var error = message != null
					? ResolvableString.transientResource(R.string.session_error_with_reason, message)
					: ResolvableString.transientResource(R.string.session_error);
			sessionDataRepository.setError(sessionId, error);
			if (failure instanceof Failure.Exceptional f) {
				Log.e("InstallViewModel", null, f.getException());
			}
		}
	}

	static final ViewModelInitializer<InstallViewModel> initializer = new ViewModelInitializer<>(
			InstallViewModel.class,
			creationExtras -> {
				final var application = creationExtras.get(APPLICATION_KEY);
				assert application != null;
				final var packageInstaller = PackageInstaller.getInstance(application);
				final var savedStateHandle = createSavedStateHandle(creationExtras);
				final var sessionsRepository = new SessionDataRepositoryImpl(savedStateHandle);
				return new InstallViewModel(packageInstaller, sessionsRepository);
			}
	);
}