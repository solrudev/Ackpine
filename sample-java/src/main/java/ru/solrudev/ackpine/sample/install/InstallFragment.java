package ru.solrudev.ackpine.sample.install;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;
import ru.solrudev.ackpine.sample.databinding.FragmentInstallBinding;
import ru.solrudev.ackpine.sample.install.InstallSessionsAdapter.SessionViewHolder;
import ru.solrudev.ackpine.sample.util.SingletonApkSequence;
import ru.solrudev.ackpine.splits.Apk;
import ru.solrudev.ackpine.splits.ApkSplits;
import ru.solrudev.ackpine.splits.ZippedApkSplits;

public final class InstallFragment extends Fragment {

	private FragmentInstallBinding binding;
	private InstallViewModel viewModel;
	private final InstallSessionsAdapter adapter = new InstallSessionsAdapter(id -> viewModel.cancelSession(id));

	private final ActivityResultLauncher<String> _pickerLauncher =
			registerForActivityResult(new ActivityResultContracts.GetContent(), this::install);

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(this, ViewModelProvider.Factory.from(InstallViewModel.initializer))
				.get(InstallViewModel.class);
	}

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentInstallBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		binding.installFab.setOnClickListener(v -> _pickerLauncher.launch("*/*"));
		binding.installRecyclerView.setAdapter(adapter);
		new ItemTouchHelper(new SwipeCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT))
				.attachToRecyclerView(binding.installRecyclerView);
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
				Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG)
						.setAnchorView(binding.installFab)
						.show();
				viewModel.clearError();
			}
		});
		viewModel.getSessionsProgress().observe(getViewLifecycleOwner(), adapter::submitProgress);
		viewModel.getSessions().observe(getViewLifecycleOwner(), adapter::submitList);
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
		final var extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
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
			} else {
				return 0;
			}
		}
	}
}