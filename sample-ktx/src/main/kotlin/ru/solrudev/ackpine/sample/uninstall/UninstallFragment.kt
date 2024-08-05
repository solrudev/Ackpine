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

package ru.solrudev.ackpine.sample.uninstall

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.sample.databinding.FragmentUninstallBinding
import ru.solrudev.ackpine.sample.util.findAppBarLayout
import ru.solrudev.ackpine.sample.util.getInstalledApplicationsCompat

class UninstallFragment : Fragment(R.layout.fragment_uninstall) {

	private val binding by viewBinding(FragmentUninstallBinding::bind, R.id.container_uninstall)
	private val viewModel: UninstallViewModel by viewModels { UninstallViewModel.Factory }

	private val adapter = ApplicationsAdapter { packageName ->
		viewModel.uninstallPackage(packageName)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		loadApplications(refresh = false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		findAppBarLayout().setLiftOnScrollTargetView(binding.recyclerViewUninstall)
		binding.root.setOnRefreshListener {
			loadApplications(refresh = true)
		}
		binding.recyclerViewUninstall.adapter = adapter
		observeViewModel()
	}

	override fun onDestroyView() {
		binding.recyclerViewUninstall.adapter = null
		super.onDestroyView()
	}

	private fun observeViewModel() = viewLifecycleOwner.lifecycleScope.launch {
		viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			viewModel.uiState.collect { uiState ->
				binding.root.isRefreshing = uiState.isLoading
				binding.textViewUninstallNoApplications.isVisible = uiState.applications.isEmpty()
				adapter.submitList(uiState.applications)
			}
		}
	}

	private fun loadApplications(refresh: Boolean) {
		val context = requireContext().applicationContext
		viewModel.loadApplications(refresh) {
			loadInstalledApplications(context)
		}
	}

	private fun loadInstalledApplications(context: Context): List<ApplicationData> {
		val packageManager = context.packageManager
		return packageManager.getInstalledApplicationsCompat(0)
			.asSequence()
			.filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
			.map { applicationInfo ->
				val icon = packageManager.getApplicationIcon(applicationInfo)
				val name = packageManager.getApplicationLabel(applicationInfo).toString()
				val packageName = applicationInfo.packageName
				ApplicationData(name, packageName, icon)
			}
			.sortedBy { it.name }
			.toList()
	}
}