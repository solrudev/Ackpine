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
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.sequences.Sequence;
import ru.solrudev.ackpine.DisposableSubscriptionContainer;
import ru.solrudev.ackpine.exceptions.SplitPackageException;
import ru.solrudev.ackpine.installer.InstallFailure;
import ru.solrudev.ackpine.installer.PackageInstaller;
import ru.solrudev.ackpine.installer.parameters.InstallParameters;
import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.session.Failure;
import ru.solrudev.ackpine.session.ProgressSession;
import ru.solrudev.ackpine.session.Session;
import ru.solrudev.ackpine.session.parameters.NotificationString;
import ru.solrudev.ackpine.splits.Apk;

public final class InstallViewModel extends ViewModel {

	private final MutableLiveData<String> error = new MutableLiveData<>();
	private final DisposableSubscriptionContainer subscriptions = new DisposableSubscriptionContainer();
	private final PackageInstaller packageInstaller;
	private final SessionDataRepository sessionDataRepository;
	private final ExecutorService executor;

	public InstallViewModel(@NonNull PackageInstaller packageInstaller,
							@NonNull SessionDataRepository sessionDataRepository,
							@NonNull ExecutorService executor) {
		this.packageInstaller = packageInstaller;
		this.sessionDataRepository = sessionDataRepository;
		this.executor = executor;
		final var sessions = sessionDataRepository.getSessions().getValue();
		if (sessions != null && !sessions.isEmpty()) {
			for (final var sessionData : sessions) {
				addSessionListeners(sessionData.id());
			}
		}
	}

	public void installPackage(@NonNull Sequence<Apk> apks, @NonNull String name) {
		executor.execute(() -> {
			final var uris = mapApkSequenceToUri(apks);
			if (uris.isEmpty()) {
				return;
			}
			final var session = packageInstaller.createSession(new InstallParameters.Builder(uris).build());
			final var sessionData = new SessionData(session.getId(), name);
			sessionDataRepository.addSessionData(sessionData);
			subscriptions.add(session.addStateListener(new SessionStateListener(session)));
			subscriptions.add(session.addProgressListener(sessionDataRepository::updateSessionProgress));
		});
	}

	@NonNull
	public LiveData<String> getError() {
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
		error.setValue("");
	}

	public void cancelSession(UUID id) {
		Futures.addCallback(packageInstaller.getSessionAsync(id), new FutureCallback<>() {
			@Override
			public void onSuccess(@Nullable ProgressSession<InstallFailure> session) {
				if (session != null) {
					session.cancel();
				}
			}

			@Override
			public void onFailure(@NonNull Throwable t) {
			}
		}, MoreExecutors.directExecutor());
	}

	public void removeSession(UUID id) {
		sessionDataRepository.removeSessionData(id);
	}

	@Override
	protected void onCleared() {
		subscriptions.clear();
		executor.shutdownNow();
	}

	private void addSessionListeners(UUID id) {
		Futures.addCallback(packageInstaller.getSessionAsync(id), new FutureCallback<>() {
			@Override
			public void onSuccess(@Nullable ProgressSession<InstallFailure> session) {
				if (session != null) {
					subscriptions.add(session.addProgressListener(sessionDataRepository::updateSessionProgress));
					subscriptions.add(session.addStateListener(new SessionStateListener(session)));
				}
			}

			@Override
			public void onFailure(@NonNull Throwable t) {
			}
		}, MoreExecutors.directExecutor());
	}

	@NonNull
	private List<Uri> mapApkSequenceToUri(@NonNull Sequence<Apk> apks) {
		try {
			final var uris = new ArrayList<Uri>();
			for (final var iterator = apks.iterator(); iterator.hasNext(); ) {
				final var apk = iterator.next();
				uris.add(apk.getUri());
			}
			return uris;
		} catch (SplitPackageException exception) {
			error.postValue(exception.getMessage());
			return Collections.emptyList();
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
				final var executor = Executors.newFixedThreadPool(8);
				return new InstallViewModel(packageInstaller, sessionsRepository, executor);
			}
	);

	private final class SessionStateListener extends Session.DefaultStateListener<InstallFailure> {

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
					? NotificationString.resource(R.string.session_error_with_reason, message)
					: NotificationString.resource(R.string.session_error);
			sessionDataRepository.setError(sessionId, error);
			if (failure instanceof Failure.Exceptional) {
				Log.e("InstallViewModel", null, ((Failure.Exceptional) failure).getException());
			}
		}
	}
}