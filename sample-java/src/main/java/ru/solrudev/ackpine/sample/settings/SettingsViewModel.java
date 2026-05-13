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

package ru.solrudev.ackpine.sample.settings;

import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
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
import java.util.concurrent.CancellationException;

public final class SettingsViewModel extends ViewModel {

	private static final String TAG = "SettingsViewModel";

	private final SettingsRepository settingsRepository;
	private final LogcatExporter logcatExporter;
	private final MutableLiveData<LogcatExportEvent> logcatExportEvent = new MutableLiveData<>(null);
	private final List<ListenableFuture<?>> futures = new ArrayList<>();

	public SettingsViewModel(@NonNull SettingsRepository settingsRepository, @NonNull LogcatExporter logcatExporter) {
		this.settingsRepository = settingsRepository;
		this.logcatExporter = logcatExporter;
	}

	@NonNull
	public LiveData<InstallerBackend> getInstallerBackend() {
		return settingsRepository.getInstallerBackendLiveData();
	}

	@NonNull
	public LiveData<Boolean> getInstallBestSuitedApks() {
		return settingsRepository.getInstallBestSuitedApksLiveData();
	}

	@NonNull
	public LiveData<LogcatExportEvent> getLogcatExportEvent() {
		return logcatExportEvent;
	}

	public void selectBackend(@NonNull InstallerBackend backend) {
		settingsRepository.setInstallerBackend(backend);
	}

	public void toggleInstallBestSuitedApks() {
		settingsRepository.toggleInstallBestSuitedApks();
	}

	public void exportLogs() {
		final var future = logcatExporter.export();
		future.addListener(() -> futures.remove(future), MoreExecutors.directExecutor());
		futures.add(future);
		Futures.addCallback(future, new FutureCallback<>() {
			@Override
			public void onSuccess(Uri uri) {
				logcatExportEvent.postValue(new LogcatExportEvent.Success(uri));
			}

			@Override
			public void onFailure(@NonNull Throwable exception) {
				if (exception instanceof CancellationException) {
					return;
				}
				Log.e(TAG, "Failed to export logs", exception);
				logcatExportEvent.postValue(LogcatExportEvent.Failure.INSTANCE);
			}
		}, MoreExecutors.directExecutor());
	}

	public void consumeLogcatExportEvent() {
		logcatExportEvent.setValue(null);
	}

	@Override
	protected void onCleared() {
		for (final var future : futures) {
			future.cancel(true);
		}
		futures.clear();
	}

	public static final ViewModelInitializer<SettingsViewModel> initializer = new ViewModelInitializer<>(
			SettingsViewModel.class,
			creationExtras -> {
				final var application = creationExtras.get(APPLICATION_KEY);
				assert application != null;
				final var preferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE);
				final var repository = new SharedPreferencesSettingsRepository(preferences);
				final var exporter = new DefaultLogcatExporter(application);
				return new SettingsViewModel(repository, exporter);
			}
	);
}