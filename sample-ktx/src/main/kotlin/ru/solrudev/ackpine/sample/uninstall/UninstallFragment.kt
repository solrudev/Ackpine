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