package ru.solrudev.ackpine.sample.uninstall;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.sample.databinding.ItemApplicationBinding;

public final class ApplicationsAdapter extends ListAdapter<ApplicationData, ApplicationsAdapter.ApplicationViewHolder> {

	private final static ApplicationDiffCallback DIFF_CALLBACK = new ApplicationDiffCallback();
	private final Consumer<String> onClick;

	public ApplicationsAdapter(Consumer<String> onClick) {
		super(DIFF_CALLBACK);
		this.onClick = onClick;
	}

	public final static class ApplicationViewHolder extends RecyclerView.ViewHolder {

		private final ItemApplicationBinding binding;
		private final Consumer<String> onClick;
		private ApplicationData currentApplicationData;

		public ApplicationViewHolder(@NonNull View itemView, Consumer<String> onClick) {
			super(itemView);
			binding = ItemApplicationBinding.bind(itemView);
			this.onClick = onClick;
			binding.buttonAppUninstall.setOnClickListener(v ->
					this.onClick.accept(currentApplicationData.packageName()));
		}

		public void bind(@NonNull ApplicationData applicationData) {
			currentApplicationData = applicationData;
			binding.imageViewAppIcon.setImageDrawable(applicationData.icon());
			binding.textViewAppName.setText(applicationData.name());
			binding.textViewAppPackageName.setText(applicationData.packageName());
		}
	}

	@NonNull
	@Override
	public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		final var view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_application, parent, false);
		return new ApplicationViewHolder(view, onClick);
	}

	@Override
	public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
		final var applicationData = getItem(position);
		holder.bind(applicationData);
	}

	private final static class ApplicationDiffCallback extends DiffUtil.ItemCallback<ApplicationData> {
		@Override
		public boolean areItemsTheSame(@NonNull ApplicationData oldItem, @NonNull ApplicationData newItem) {
			return oldItem.packageName().equals(newItem.packageName());
		}

		@Override
		public boolean areContentsTheSame(@NonNull ApplicationData oldItem, @NonNull ApplicationData newItem) {
			return oldItem.equals(newItem);
		}
	}
}
