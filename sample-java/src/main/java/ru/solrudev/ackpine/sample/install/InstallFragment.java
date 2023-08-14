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
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.GetContent;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewKt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;
import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.sample.databinding.FragmentInstallBinding;
import ru.solrudev.ackpine.sample.install.InstallSessionsAdapter.SessionViewHolder;
import ru.solrudev.ackpine.splits.Apk;
import ru.solrudev.ackpine.splits.ApkSplits;
import ru.solrudev.ackpine.splits.ZippedApkSplits;

public final class InstallFragment extends Fragment {

	private FragmentInstallBinding binding;
	private InstallViewModel viewModel;
	private final InstallSessionsAdapter adapter = new InstallSessionsAdapter(id -> viewModel.cancelSession(id));

	@RequiresApi(Build.VERSION_CODES.M)
	private final ActivityResultLauncher<String> requestPermissionLauncher =
			registerForActivityResult(new RequestPermission(), isGranted -> {
			});

	private final ActivityResultLauncher<String> pickerLauncher =
			registerForActivityResult(new GetContent(), this::install);

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
		new ItemTouchHelper(new SwipeCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT))
				.attachToRecyclerView(binding.recyclerViewInstall);
		observeViewModel();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	private void observeViewModel() {
		viewModel.getError().observe(getViewLifecycleOwner(), error -> {
			if (!error.isEmpty()) {
				Snackbar.make(requireView(), error.resolve(requireContext()), Snackbar.LENGTH_LONG)
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
		if (!allPermissionsGranted()) {
			requestPermissions();
			return;
		}
		try {
			pickerLauncher.launch("*/*");
		} catch (ActivityNotFoundException ignored) {
		}
	}

	private void install(@Nullable Uri uri) {
		if (uri == null) {
			return;
		}
		final var name = getDisplayName(requireContext().getContentResolver(), uri);
		final var apks = getApksFromUri(uri, name);
		viewModel.installPackage(apks, name);
	}

	@NonNull
	private Sequence<Apk> getApksFromUri(@NonNull Uri uri, @NonNull String name) {
		final var extensionIndex = name.lastIndexOf('.') + 1;
		final var extension = extensionIndex != 0 ? name.substring(extensionIndex).toLowerCase() : "";
		return switch (extension) {
			case "apk" -> new SingletonApkSequence(uri, requireContext());
			case "zip", "apks", "xapk", "apkm" -> ApkSplits.throwOnInvalidSplitPackage(
					ApkSplits.filterIncompatible(ZippedApkSplits.getApksForUri(uri, requireContext()),
							requireContext()));
			default -> SequencesKt.emptySequence();
		};
	}

	@NonNull
	private static String getDisplayName(@NonNull ContentResolver resolver, @NonNull Uri uri) {
		try (final var cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
			if (cursor == null) return "";
			if (!cursor.moveToFirst()) return "";
			return cursor.getString(0);
		}
	}

	private void requestPermissions() {
		requestReadStoragePermission();
		requestManageAllFilesPermission();
		requestNotificationPermission();
	}

	private void requestReadStoragePermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
			requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE);
		}
	}

	private void requestManageAllFilesPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
			startActivity(new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
					.setData(Uri.parse("package:" + requireContext().getPackageName())));
		}
	}

	private void requestNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			requestPermissionLauncher.launch(POST_NOTIFICATIONS);
		}
	}

	private boolean allPermissionsGranted() {
		final var readStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
				|| Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
				|| requireContext().checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		final var storageManager = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
				|| Environment.isExternalStorageManager();
		final var notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
				|| requireContext().checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
		return readStorage && storageManager && notifications;
	}

	private final class SwipeCallback extends ItemTouchHelper.SimpleCallback {

		public SwipeCallback(int dragDirs, int swipeDirs) {
			super(dragDirs, swipeDirs);
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView,
							  @NonNull RecyclerView.ViewHolder viewHolder,
							  @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			viewModel.removeSession(((SessionViewHolder) viewHolder).getSessionId());
		}

		@Override
		public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
			if (((SessionViewHolder) viewHolder).isSwipeable()) {
				return super.getSwipeDirs(recyclerView, viewHolder);
			}
			return 0;
		}
	}
}