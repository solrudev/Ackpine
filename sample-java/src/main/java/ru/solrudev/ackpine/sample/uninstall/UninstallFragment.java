/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.sample.uninstall;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewKt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.sample.databinding.FragmentUninstallBinding;

public final class UninstallFragment extends Fragment {

	private FragmentUninstallBinding binding;
	private UninstallViewModel viewModel;

	private final ApplicationsAdapter adapter =
			new ApplicationsAdapter(packageName -> viewModel.uninstallPackage(packageName));

	public UninstallFragment() {
		super(R.layout.fragment_uninstall);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(this, ViewModelProvider.Factory.from(UninstallViewModel.initializer))
				.get(UninstallViewModel.class);
		loadApplications(false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		binding = FragmentUninstallBinding.bind(view);
		requireActivity().<AppBarLayout>findViewById(R.id.appBarLayout_nav_host)
				.setLiftOnScrollTargetView(binding.recyclerViewUninstall);
		binding.getRoot().setOnRefreshListener(() -> loadApplications(true));
		binding.recyclerViewUninstall.setAdapter(adapter);
		observeViewModel();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	private void observeViewModel() {
		viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> binding.getRoot().setRefreshing(isLoading));
		viewModel.getApplications().observe(getViewLifecycleOwner(), list -> {
			ViewKt.setVisible(binding.textViewUninstallNoApplications, list.isEmpty());
			adapter.submitList(list);
		});
	}

	private void loadApplications(boolean refresh) {
		final var context = requireContext().getApplicationContext();
		viewModel.loadApplications(refresh, () -> loadInstalledApplications(context));
	}

	@NonNull
	private List<ApplicationData> loadInstalledApplications(@NonNull Context context) {
		final var packageManager = context.getPackageManager();
		//noinspection RedundantSuppression
		@SuppressWarnings("deprecation") final var apps = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
				? packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
				: packageManager.getInstalledApplications(0);
		final var applications = new ArrayList<ApplicationData>();
		for (final var applicationInfo : apps) {
			if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
				continue;
			}
			final var icon = packageManager.getApplicationIcon(applicationInfo);
			final var name = packageManager.getApplicationLabel(applicationInfo).toString();
			final var packageName = applicationInfo.packageName;
			applications.add(new ApplicationData(name, packageName, icon));
		}
		Collections.sort(applications, (first, second) -> first.name().compareTo(second.name()));
		return applications;
	}
}