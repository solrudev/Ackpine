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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.sample.databinding.FragmentInstallBinding
import ru.solrudev.ackpine.sample.install.InstallSessionsAdapter.SessionViewHolder
import ru.solrudev.ackpine.sample.util.findAppBarLayout
import ru.solrudev.ackpine.sample.util.getDisplayName
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.ApkSplits.filterCompatible
import ru.solrudev.ackpine.splits.ApkSplits.throwOnInvalidSplitPackage
import ru.solrudev.ackpine.splits.ZippedApkSplits

class InstallFragment : Fragment(R.layout.fragment_install) {

	private val binding by viewBinding(FragmentInstallBinding::bind, R.id.container_install)
	private val viewModel: InstallViewModel by viewModels { InstallViewModel.Factory }

	private val adapter = InstallSessionsAdapter { sessionId ->
		viewModel.cancelSession(sessionId)
	}

	@RequiresApi(Build.VERSION_CODES.M)
	private val requestPermissionsLauncher = registerForActivityResult(RequestMultiplePermissions()) { results ->
		if (results.values.all { it }) {
			chooseFile()
		}
	}

	private val pickerLauncher = registerForActivityResult(GetContent(), ::install)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		findAppBarLayout().setLiftOnScrollTargetView(binding.recyclerViewInstall)
		binding.fabInstall.setOnClickListener {
			onInstallButtonClick()
		}
		binding.recyclerViewInstall.adapter = adapter
		ItemTouchHelper(SwipeCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT))
			.attachToRecyclerView(binding.recyclerViewInstall)
		observeViewModel()
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

	private fun onInstallButtonClick() {
		if (!allPermissionsGranted()) {
			requestPermissions()
			return
		}
		chooseFile()
	}

	private fun chooseFile() {
		try {
			pickerLauncher.launch("*/*")
		} catch (_: ActivityNotFoundException) {
		}
	}

	fun install(uri: Uri?) {
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

	private fun requestPermissions() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return
		}
		val permissions = mutableSetOf<String>()
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
			permissions += READ_EXTERNAL_STORAGE
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			permissions += POST_NOTIFICATIONS
		}
		requestPermissionsLauncher.launch(permissions.toTypedArray())
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

	private inner class SwipeCallback(
		dragDirs: Int,
		swipeDirs: Int
	) : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		) = false

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			viewModel.removeSession((viewHolder as SessionViewHolder).sessionId)
		}

		override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
			if ((viewHolder as SessionViewHolder).isSwipeable) {
				return super.getSwipeDirs(recyclerView, viewHolder)
			}
			return 0
		}
	}
}