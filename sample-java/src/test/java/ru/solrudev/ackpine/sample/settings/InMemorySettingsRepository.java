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

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public final class InMemorySettingsRepository implements SettingsRepository {

	private final Supplier<Boolean> supportsShizuku;
	private final MutableLiveData<InstallerBackend> installerBackendLiveData;
	private final MutableLiveData<Boolean> installBestSuitedApksLiveData;
	private InstallerBackend backend;
	private boolean installBestSuitedApks;

	public InMemorySettingsRepository(
			@NonNull InstallerBackend backend,
			@NonNull Supplier<Boolean> supportsShizuku
	) {
		this(backend, supportsShizuku, true);
	}

	public InMemorySettingsRepository(
			@NonNull InstallerBackend backend,
			@NonNull Supplier<Boolean> supportsShizuku,
			boolean installBestSuitedApks
	) {
		this.supportsShizuku = supportsShizuku;
		this.backend = sanitize(backend);
		this.installerBackendLiveData = new MutableLiveData<>(this.backend);
		this.installBestSuitedApks = installBestSuitedApks;
		this.installBestSuitedApksLiveData = new MutableLiveData<>(installBestSuitedApks);
	}

	@NonNull
	@Override
	public InstallerBackend getInstallerBackend() {
		return backend;
	}

	@Override
	public void setInstallerBackend(@NonNull InstallerBackend backend) {
		this.backend = sanitize(backend);
		installerBackendLiveData.setValue(this.backend);
	}

	@NonNull
	@Override
	public LiveData<InstallerBackend> getInstallerBackendLiveData() {
		return installerBackendLiveData;
	}

	@Override
	public boolean isInstallBestSuitedApks() {
		return installBestSuitedApks;
	}

	@NonNull
	@Override
	public LiveData<Boolean> getInstallBestSuitedApksLiveData() {
		return installBestSuitedApksLiveData;
	}

	@Override
	public void toggleInstallBestSuitedApks() {
		installBestSuitedApks = !installBestSuitedApks;
		installBestSuitedApksLiveData.setValue(installBestSuitedApks);
	}

	@NonNull
	private InstallerBackend sanitize(@NonNull InstallerBackend backend) {
		return backend == InstallerBackend.SHIZUKU && Boolean.FALSE.equals(supportsShizuku.get())
				? InstallerBackend.ROOTLESS
				: backend;
	}
}