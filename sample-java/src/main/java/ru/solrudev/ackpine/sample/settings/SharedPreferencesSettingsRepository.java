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
import androidx.lifecycle.MutableLiveData;

import rikka.shizuku.Shizuku;

public final class SharedPreferencesSettingsRepository implements SettingsRepository {

	private static final String INSTALLER_BACKEND_KEY = "installer_backend";
	private static final String INSTALL_BEST_SUITED_APKS_KEY = "install_best_suited_apks";
	private final SharedPreferences sharedPreferences;
	private final LiveData<InstallerBackend> installerBackendLiveData;
	private final MutableLiveData<Boolean> installBestSuitedApksLiveData;

	public SharedPreferencesSettingsRepository(@NonNull SharedPreferences sharedPreferences) {
		this.sharedPreferences = sharedPreferences;
		this.installBestSuitedApksLiveData = new MutableLiveData<>(isInstallBestSuitedApks());
		final var liveData = new LiveData<InstallerBackend>() {
			private final SharedPreferences.OnSharedPreferenceChangeListener listener = (preferences, key) -> {
				if (INSTALLER_BACKEND_KEY.equals(key)) {
					setValue(getInstallerBackend());
				}
			};

			@Override
			protected void onActive() {
				sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
				setValue(getInstallerBackend());
			}

			@Override
			protected void onInactive() {
				sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
			}

			void setBackend(InstallerBackend value) {
				setValue(value);
			}
		};
		this.installerBackendLiveData = liveData;
		Shizuku.addBinderReceivedListener(() -> liveData.setBackend(getInstallerBackend()));
		Shizuku.addBinderDeadListener(() -> liveData.setBackend(getInstallerBackend()));
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
		installBestSuitedApksLiveData.setValue(newValue);
	}

	private boolean isShizukuAvailable() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Shizuku.pingBinder();
	}
}