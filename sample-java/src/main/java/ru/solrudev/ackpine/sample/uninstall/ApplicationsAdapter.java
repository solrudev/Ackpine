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

package ru.solrudev.ackpine.sample.uninstall;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

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

		public ApplicationViewHolder(@NonNull ItemApplicationBinding itemBinding, Consumer<String> onClick) {
			super(itemBinding.getRoot());
			binding = itemBinding;
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
		final var itemBinding = ItemApplicationBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new ApplicationViewHolder(itemBinding, onClick);
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
