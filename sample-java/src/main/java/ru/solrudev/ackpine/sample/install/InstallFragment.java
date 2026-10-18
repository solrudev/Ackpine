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

package ru.solrudev.ackpine.sample.install;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments;
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.os.BundleCompat;
import androidx.core.view.ViewKt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.sample.databinding.FragmentInstallBinding;
import ru.solrudev.ackpine.splits.ApkSplits;
import ru.solrudev.ackpine.splits.SplitPackage;
import ru.solrudev.ackpine.splits.ZippedApkSplits;

public final class InstallFragment extends Fragment {

	public static final String URI_KEY = "URI";
	private FragmentInstallBinding binding;
	private InstallViewModel viewModel;

	private final InstallSessionsAdapter adapter = new InstallSessionsAdapter(id -> viewModel.cancelSession(id),
			id -> viewModel.removeSession(id));

	@RequiresApi(Build.VERSION_CODES.M)
	private final ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(
			new RequestMultiplePermissions(),
			results -> {
				for (final var isGranted : results.values()) {
					if (!isGranted) {
						return;
					}
				}
				chooseFile();
			});

	@RequiresApi(Build.VERSION_CODES.M)
	private final ActivityResultLauncher<String[]> requestPermissionsActionViewLauncher = registerForActivityResult(
			new RequestMultiplePermissions(),
			results -> {
				for (final var isGranted : results.values()) {
					if (!isGranted) {
						resetUriToInstall();
						return;
					}
				}
				install(getUriToInstall());
				resetUriToInstall();
			});

	private final ActivityResultLauncher<String[]> pickerLauncher =
			registerForActivityResult(new OpenMultipleDocuments(), this::install);

	public InstallFragment() {
		super(R.layout.fragment_install);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(this, ViewModelProvider.Factory.from(InstallViewModel.initializer))
				.get(InstallViewModel.class);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		binding = FragmentInstallBinding.bind(view);
		requireActivity().<AppBarLayout>findViewById(R.id.appBarLayout_nav_host)
				.setLiftOnScrollTargetView(binding.recyclerViewInstall);
		binding.fabInstall.setOnClickListener(v -> onInstallButtonClick());
		binding.recyclerViewInstall.setAdapter(adapter);
		observeViewModel();
		if (getUriToInstall() != null) {
			onActionView();
		}
	}

	@Override
	public void onDestroyView() {
		binding.recyclerViewInstall.setAdapter(null);
		binding = null;
		super.onDestroyView();
	}

	@Nullable
	private Uri getUriToInstall() {
		final var arguments = getArguments();
		if (arguments == null) {
			return null;
		}
		return BundleCompat.getParcelable(arguments, URI_KEY, Uri.class);
	}

	private void resetUriToInstall() {
		final var arguments = getArguments();
		if (arguments == null) {
			return;
		}
		arguments.remove(URI_KEY);
		setArguments(arguments);
	}

	private void observeViewModel() {
		viewModel.getError().observe(getViewLifecycleOwner(), error -> {
			if (!error.isEmpty()) {
				Snackbar.make(requireActivity().findViewById(R.id.content_nav_host),
								error.resolve(requireContext()),
								Snackbar.LENGTH_LONG)
						.setAnchorView(binding.fabInstall)
						.show();
				viewModel.clearError();
			}
		});
		viewModel.getSessionsProgress().observe(getViewLifecycleOwner(), adapter::submitProgress);
		viewModel.getSessions().observe(getViewLifecycleOwner(), list -> {
			ViewKt.setVisible(binding.textViewInstallNoSessions, list.isEmpty());
			adapter.submitList(list);
		});
	}

	private void onInstallButtonClick() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || allPermissionsGranted()) {
			chooseFile();
			return;
		}
		requestPermissionsLauncher.launch(getRequiredPermissions().toArray(new String[]{}));
	}

	private void onActionView() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || allPermissionsGranted()) {
			install(getUriToInstall());
			resetUriToInstall();
			return;
		}
		requestPermissionsActionViewLauncher.launch(getRequiredPermissions().toArray(new String[]{}));
	}

	private void chooseFile() {
		try {
			pickerLauncher.launch(new String[]{PickedFile.APK_MIME_TYPE, PickedFile.ZIP_MIME_TYPE,
					PickedFile.BINARY_MIME_TYPE});
		} catch (ActivityNotFoundException ignored) { // no-op
		}
	}

	private void install(@Nullable Uri uri) {
		install(uri != null ? List.of(uri) : List.of());
	}

	private void install(@NonNull List<Uri> uris) {
		if (uris.isEmpty()) {
			return;
		}
		final var context = requireContext();
		final var signatureFiles = new ArrayList<PickedFile>();
		final var packageFiles = new ArrayList<PickedFile>();
		for (final var uri : uris) {
			final var file = PickedFile.from(uri, context);
			(file.isV4Signature() ? signatureFiles : packageFiles).add(file);
		}
		if (packageFiles.isEmpty()) {
			viewModel.installPackage(SplitPackage.empty(), signatureFiles.get(0).getName());
			return;
		}
		final var name = packageFiles.get(0).getName();
		final var pickedSignatures = V4Signatures.fromPickedFiles(packageFiles, signatureFiles);
		if (packageFiles.size() == 1 && packageFiles.get(0).isArchive()) {
			final var archiveUri = packageFiles.get(0).getUri();
			final var splitPackage = SplitPackage
					.from(ApkSplits.validate(ZippedApkSplits.getApksForUri(archiveUri, context)))
					.sortedByCompatibility(context);
			// Signatures picked as separate files take precedence over the ones inside the archive.
			viewModel.installPackage(splitPackage, name, () -> {
				final var signatures = new HashMap<>(V4Signatures.fromZip(archiveUri, context));
				signatures.putAll(pickedSignatures);
				return signatures;
			});
			return;
		}
		// Everything that's not a lone archive is treated as APKs, so several separately picked splits install as
		// one package. Files which aren't APKs are skipped when the sequence is iterated.
		final var apkUris = new ArrayList<Uri>();
		for (final var file : packageFiles) {
			apkUris.add(file.getUri());
		}
		var splitPackage = SplitPackage.from(new UriApkSequence(apkUris, context));
		if (packageFiles.size() > 1) {
			splitPackage = splitPackage.sortedByCompatibility(context);
		}
		viewModel.installPackage(splitPackage, name, () -> pickedSignatures);
	}

	@RequiresApi(Build.VERSION_CODES.M)
	@NonNull
	private HashSet<String> getRequiredPermissions() {
		final var permissions = new HashSet<String>();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			permissions.add(POST_NOTIFICATIONS);
		}
		return permissions;
	}

	private boolean allPermissionsGranted() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		}
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
				|| requireContext().checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
	}
}