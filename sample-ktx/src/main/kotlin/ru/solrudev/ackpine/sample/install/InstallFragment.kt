/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

package ru.solrudev.ackpine.sample.install

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresApi
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.sample.databinding.FragmentInstallBinding
import ru.solrudev.ackpine.sample.util.findAppBarLayout
import ru.solrudev.ackpine.sample.util.getDisplayName
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.ApkSplits.filterCompatible
import ru.solrudev.ackpine.splits.ApkSplits.throwOnInvalidSplitPackage
import ru.solrudev.ackpine.splits.ZippedApkSplits

class InstallFragment : Fragment(R.layout.fragment_install) {

	private val binding by viewBinding(FragmentInstallBinding::bind, R.id.container_install)
	private val viewModel: InstallViewModel by viewModels { InstallViewModel.Factory }

	private val uriToInstall: Uri?
		get() = arguments?.let { BundleCompat.getParcelable(it, URI_KEY, Uri::class.java) }

	private val adapter = InstallSessionsAdapter(
		onCancelClick = { sessionId ->
			viewModel.cancelSession(sessionId)
		},
		onItemSwipe = { sessionId ->
			viewModel.removeSession(sessionId)
		}
	)

	@RequiresApi(Build.VERSION_CODES.M)
	private val requestPermissionsLauncher = registerForActivityResult(
		RequestMultiplePermissions()
	) { results ->
		if (results.values.all { it }) {
			chooseFile()
		}
	}

	@RequiresApi(Build.VERSION_CODES.M)
	private val requestPermissionsActionViewLauncher = registerForActivityResult(
		RequestMultiplePermissions()
	) { results ->
		if (results.values.all { it }) {
			install(uriToInstall)
		}
		resetUriToInstall()
	}

	private val pickerLauncher = registerForActivityResult(GetContent(), ::install)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		findAppBarLayout().setLiftOnScrollTargetView(binding.recyclerViewInstall)
		binding.fabInstall.setOnClickListener {
			onInstallButtonClick()
		}
		binding.recyclerViewInstall.adapter = adapter
		observeViewModel()
		if (uriToInstall != null) {
			onActionView()
		}
	}

	override fun onDestroyView() {
		binding.recyclerViewInstall.adapter = null
		super.onDestroyView()
	}

	private fun observeViewModel() = viewLifecycleOwner.lifecycleScope.launch {
		viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			viewModel.uiState.collect { uiState ->
				if (!uiState.error.isEmpty) {
					Snackbar.make(requireView(), uiState.error.resolve(requireContext()), Snackbar.LENGTH_LONG)
						.setAnchorView(binding.fabInstall)
						.show()
					viewModel.clearError()
				}
				binding.textViewInstallNoSessions.isVisible = uiState.sessions.isEmpty()
				adapter.submitProgress(uiState.sessionsProgress)
				adapter.submitList(uiState.sessions)
			}
		}
	}

	private fun resetUriToInstall() {
		val bundle = arguments ?: return
		bundle.remove(URI_KEY)
		arguments = bundle
	}

	private fun onInstallButtonClick() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || allPermissionsGranted()) {
			chooseFile()
			return
		}
		requestPermissionsLauncher.launch(getRequiredPermissions().toTypedArray())
	}

	private fun onActionView() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || allPermissionsGranted()) {
			install(uriToInstall)
			resetUriToInstall()
			return
		}
		requestPermissionsActionViewLauncher.launch(getRequiredPermissions().toTypedArray())
	}

	private fun chooseFile() {
		try {
			pickerLauncher.launch("*/*")
		} catch (_: ActivityNotFoundException) { // no-op
		}
	}

	private fun install(uri: Uri?) {
		if (uri == null) {
			return
		}
		val name = requireContext().contentResolver.getDisplayName(uri)
		val apks = getApksFromUri(uri, name)
		viewModel.installPackage(apks, name)
	}

	private fun getApksFromUri(uri: Uri, name: String): Sequence<Apk> {
		val extension = name.substringAfterLast('.', "").lowercase()
		val context = requireContext().applicationContext
		return when (extension) {
			"apk" -> sequence { Apk.fromUri(uri, context)?.let { yield(it) } }.constrainOnce()
			"zip", "apks", "xapk", "apkm" -> ZippedApkSplits.getApksForUri(uri, requireContext())
				.throwOnInvalidSplitPackage()
				.filterCompatible(requireContext())

			else -> emptySequence()
		}
	}

	@RequiresApi(Build.VERSION_CODES.M)
	private fun getRequiredPermissions() = buildSet {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
			add(READ_EXTERNAL_STORAGE)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			add(POST_NOTIFICATIONS)
		}
	}

	private fun allPermissionsGranted(): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true
		}
		val readStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
				|| requireContext().checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
		val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
				|| requireContext().checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
		return readStorage && notifications
	}

	companion object {
		const val URI_KEY = "URI"
	}
}