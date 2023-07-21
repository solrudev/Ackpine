package ru.solrudev.ackpine.sample.uninstall;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.solrudev.ackpine.sample.databinding.FragmentUninstallBinding;

public final class UninstallFragment extends Fragment {

	private FragmentUninstallBinding binding;
	private UninstallViewModel viewModel;

	private final ApplicationsAdapter adapter =
			new ApplicationsAdapter(packageName -> viewModel.uninstallPackage(packageName));

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(this, ViewModelProvider.Factory.from(UninstallViewModel.initializer))
				.get(UninstallViewModel.class);
		if (savedInstanceState == null) {
			loadApplications();
		}
	}

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentUninstallBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		binding.getRoot().setOnRefreshListener(this::loadApplications);
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
		viewModel.getApplications().observe(getViewLifecycleOwner(), adapter::submitList);
	}

	private void loadApplications() {
		final var context = requireContext().getApplicationContext();
		viewModel.loadApplications(() -> loadInstalledApplications(context));
	}

	@NonNull
	private List<ApplicationData> loadInstalledApplications(@NonNull Context context) {
		final var packageManager = context.getPackageManager();
		final var apps = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
				? packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
				: packageManager.getInstalledApplications(0);
		final var applications = new ArrayList<ApplicationData>();
		var index = 0;
		for (final var applicationInfo : apps) {
			if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
				continue;
			}
			final var icon = packageManager.getApplicationIcon(applicationInfo);
			final var name = packageManager.getApplicationLabel(applicationInfo).toString();
			final var packageName = applicationInfo.packageName;
			applications.add(new ApplicationData(index++, name, packageName, icon));
		}
		Collections.sort(applications, (first, second) -> first.name().compareTo(second.name()));
		return applications;
	}
}