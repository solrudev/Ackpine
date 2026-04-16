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

import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import rikka.shizuku.Shizuku;

public final class SharedPreferencesSettingsRepository implements SettingsRepository {

	private static final String INSTALLER_BACKEND_KEY = "installer_backend";
	private static final String INSTALL_BEST_SUITED_APKS_KEY = "install_best_suited_apks";
	private final SharedPreferences sharedPreferences;
	private final SharedPreferenceLiveData<InstallerBackend> installerBackendLiveData;
	private final SharedPreferenceLiveData<Boolean> installBestSuitedApksLiveData;

	public SharedPreferencesSettingsRepository(@NonNull SharedPreferences sharedPreferences) {
		this.sharedPreferences = sharedPreferences;
		installerBackendLiveData = new SharedPreferenceLiveData<>(sharedPreferences,
				INSTALLER_BACKEND_KEY,
				this::getInstallerBackend);
		installBestSuitedApksLiveData = new SharedPreferenceLiveData<>(sharedPreferences,
				INSTALL_BEST_SUITED_APKS_KEY,
				this::isInstallBestSuitedApks);
		Shizuku.addBinderReceivedListener(installerBackendLiveData::refresh);
		Shizuku.addBinderDeadListener(installerBackendLiveData::refresh);
	}

	@NonNull
	@Override
	public InstallerBackend getInstallerBackend() {
		final var value = sharedPreferences.getString(INSTALLER_BACKEND_KEY, null);
		if (value == null || value.equals(InstallerBackend.SHIZUKU.name()) && !isShizukuAvailable()) {
			return InstallerBackend.ROOTLESS;
		}
		return InstallerBackend.valueOf(value);
	}

	@Override
	public void setInstallerBackend(@NonNull InstallerBackend backend) {
		sharedPreferences.edit().putString(INSTALLER_BACKEND_KEY, backend.name()).apply();
	}

	@NonNull
	@Override
	public LiveData<InstallerBackend> getInstallerBackendLiveData() {
		return installerBackendLiveData;
	}

	@Override
	public boolean isInstallBestSuitedApks() {
		return sharedPreferences.getBoolean(INSTALL_BEST_SUITED_APKS_KEY, true);
	}

	@NonNull
	@Override
	public LiveData<Boolean> getInstallBestSuitedApksLiveData() {
		return installBestSuitedApksLiveData;
	}

	@Override
	public void toggleInstallBestSuitedApks() {
		final var newValue = !isInstallBestSuitedApks();
		sharedPreferences.edit().putBoolean(INSTALL_BEST_SUITED_APKS_KEY, newValue).apply();
	}

	private boolean isShizukuAvailable() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Shizuku.pingBinder();
	}
}