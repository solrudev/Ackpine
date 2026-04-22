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

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;

import rikka.shizuku.Shizuku;
import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.sample.databinding.FragmentSettingsBinding;

public final class SettingsFragment extends Fragment {

	private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;
	private FragmentSettingsBinding binding;
	private SettingsViewModel viewModel;

	private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, result) -> {
		if (requestCode != SHIZUKU_PERMISSION_REQUEST_CODE) {
			return;
		}
		if (result == PackageManager.PERMISSION_GRANTED) {
			viewModel.selectBackend(InstallerBackend.SHIZUKU);
		} else {
			showToast(R.string.shizuku_permission_denied);
		}
	};

	public SettingsFragment() {
		super(R.layout.fragment_settings);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(this, ViewModelProvider.Factory.from(SettingsViewModel.initializer))
				.get(SettingsViewModel.class);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		binding = FragmentSettingsBinding.bind(view);
		requireActivity().<AppBarLayout>findViewById(R.id.appBarLayout_nav_host)
				.setLiftOnScrollTargetView(binding.scrollViewSettings);
		binding.layoutSettingsInstallerRootless.setOnClickListener(v -> {
			viewModel.selectBackend(InstallerBackend.ROOTLESS);
		});
		binding.layoutSettingsInstallerRoot.setOnClickListener(v -> {
			viewModel.selectBackend(InstallerBackend.ROOT);
		});
		binding.layoutSettingsInstallerShizuku.setOnClickListener(v -> selectShizukuBackend());
		binding.layoutSettingsInstallBestSuitedApks.setOnClickListener(v -> {
			viewModel.toggleInstallBestSuitedApks();
		});
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			Shizuku.addRequestPermissionResultListener(permissionListener);
		}
		observeViewModel();
	}

	@Override
	public void onDestroyView() {
		try {
			Shizuku.removeRequestPermissionResultListener(permissionListener);
		} catch (NoClassDefFoundError ignore) { // ignore
		}
		binding = null;
		super.onDestroyView();
	}

	private void observeViewModel() {
		viewModel.getInstallerBackend().observe(getViewLifecycleOwner(), backend -> {
			binding.radioButtonSettingsInstallerRootless.setChecked(backend == InstallerBackend.ROOTLESS);
			binding.radioButtonSettingsInstallerRoot.setChecked(backend == InstallerBackend.ROOT);
			binding.radioButtonSettingsInstallerShizuku.setChecked(backend == InstallerBackend.SHIZUKU);
		});
		viewModel.getInstallBestSuitedApks().observe(getViewLifecycleOwner(), enabled -> {
			binding.switchSettingsInstallBestSuitedApks.setChecked(enabled);
		});
	}

	private void selectShizukuBackend() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			showToast(R.string.shizuku_unsupported_android_version);
			return;
		}
		try {
			if (!Shizuku.pingBinder()) {
				showToast(R.string.shizuku_not_running);
				return;
			}
			if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
				viewModel.selectBackend(InstallerBackend.SHIZUKU);
				return;
			}
			Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
		} catch (IllegalStateException ignored) {
			showToast(R.string.shizuku_not_running);
		}
	}

	private void showToast(int messageRes) {
		Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
	}
}