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

package ru.solrudev.ackpine.sample.settings

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.sample.databinding.FragmentSettingsBinding
import ru.solrudev.ackpine.sample.util.findAppBarLayout

private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1

class SettingsFragment : Fragment(R.layout.fragment_settings) {

	private val binding by viewBinding(FragmentSettingsBinding::bind, R.id.scrollView_settings)
	private val viewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

	private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
		if (requestCode != SHIZUKU_PERMISSION_REQUEST_CODE) {
			return@OnRequestPermissionResultListener
		}
		if (grantResult == PackageManager.PERMISSION_GRANTED) {
			viewModel.selectBackend(InstallerBackend.SHIZUKU)
		} else {
			showToast(R.string.shizuku_permission_denied)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		findAppBarLayout().setLiftOnScrollTargetView(binding.scrollViewSettings)
		binding.layoutSettingsInstallerRootless.setOnClickListener {
			viewModel.selectBackend(InstallerBackend.ROOTLESS)
		}
		binding.layoutSettingsInstallerRoot.setOnClickListener {
			viewModel.selectBackend(InstallerBackend.ROOT)
		}
		binding.layoutSettingsInstallerShizuku.setOnClickListener {
			selectShizukuBackend()
		}
		binding.layoutSettingsInstallBestSuitedApks.setOnClickListener {
			viewModel.toggleInstallBestSuitedApks()
		}
		Shizuku.addRequestPermissionResultListener(permissionListener)
		this@SettingsFragment.observeViewModel()
	}

	override fun onDestroyView() {
		Shizuku.removeRequestPermissionResultListener(permissionListener)
		super.onDestroyView()
	}

	private fun observeViewModel() = viewLifecycleOwner.lifecycleScope.launch {
		viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			viewModel.uiState.collect { uiState ->
				renderInstallerBackend(uiState.installerBackend)
				binding.switchSettingsInstallBestSuitedApks.isChecked = uiState.installBestSuitedApks
			}
		}
	}

	private fun renderInstallerBackend(backend: InstallerBackend) {
		binding.radioButtonSettingsInstallerRootless.isChecked = backend == InstallerBackend.ROOTLESS
		binding.radioButtonSettingsInstallerRoot.isChecked = backend == InstallerBackend.ROOT
		binding.radioButtonSettingsInstallerShizuku.isChecked = backend == InstallerBackend.SHIZUKU
	}

	private fun selectShizukuBackend() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			showToast(R.string.shizuku_unsupported_android_version)
			return
		}
		try {
			if (!Shizuku.pingBinder()) {
				showToast(R.string.shizuku_not_running)
				return
			}
			if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
				viewModel.selectBackend(InstallerBackend.SHIZUKU)
				return
			}
			Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
		} catch (_: IllegalStateException) {
			showToast(R.string.shizuku_not_running)
		}
	}

	private fun showToast(messageRes: Int) {
		Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
	}
}