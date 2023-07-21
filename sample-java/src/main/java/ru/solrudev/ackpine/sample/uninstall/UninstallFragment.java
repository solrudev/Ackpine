package ru.solrudev.ackpine.sample.uninstall;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import ru.solrudev.ackpine.sample.databinding.FragmentUninstallBinding;

public class UninstallFragment extends Fragment {

	private FragmentUninstallBinding binding;

	public View onCreateView(@NonNull LayoutInflater inflater,
							 ViewGroup container, Bundle savedInstanceState) {
		UninstallViewModel uninstallViewModel = new ViewModelProvider(
				this,
				ViewModelProvider.Factory.from(UninstallViewModel.initializer)
		).get(UninstallViewModel.class);
		binding = FragmentUninstallBinding.inflate(inflater, container, false);
		View root = binding.getRoot();
		final TextView textView = binding.textDashboard;
		uninstallViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
		return root;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}