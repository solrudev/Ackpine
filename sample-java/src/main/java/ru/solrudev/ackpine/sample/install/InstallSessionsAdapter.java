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

package ru.solrudev.ackpine.sample.install;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.view.ViewKt;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.sample.databinding.ItemInstallSessionBinding;
import ru.solrudev.ackpine.session.Progress;
import ru.solrudev.ackpine.session.parameters.NotificationString;

public final class InstallSessionsAdapter extends ListAdapter<SessionData, InstallSessionsAdapter.SessionViewHolder> {

	private static final SessionDiffCallback DIFF_CALLBACK = new SessionDiffCallback();
	private static final ItemAnimator ITEM_ANIMATOR = new ItemAnimator();
	private final Consumer<UUID> onCancelClick;
	private final Consumer<UUID> onItemSwipe;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private boolean isReattaching = false;
	private List<SessionProgress> currentProgress = Collections.emptyList();

	public InstallSessionsAdapter(Consumer<UUID> onCancelClick, Consumer<UUID> onItemSwipe) {
		super(DIFF_CALLBACK);
		this.onCancelClick = onCancelClick;
		this.onItemSwipe = onItemSwipe;
	}

	public static final class SessionViewHolder extends RecyclerView.ViewHolder {

		private final ItemInstallSessionBinding binding;
		private final Consumer<UUID> onClick;
		private SessionData currentSessionData;

		public SessionViewHolder(@NonNull ItemInstallSessionBinding itemBinding, Consumer<UUID> onClick) {
			super(itemBinding.getRoot());
			binding = itemBinding;
			this.onClick = onClick;
			binding.buttonSessionCancel.setOnClickListener(v -> this.onClick.accept(currentSessionData.id()));
		}

		public boolean isSwipeable() {
			if (currentSessionData == null) {
				return false;
			}
			return !currentSessionData.error().isEmpty();
		}

		@Nullable
		public UUID getSessionId() {
			if (currentSessionData == null) {
				return null;
			}
			return currentSessionData.id();
		}

		public void bind(@NonNull SessionData sessionData) {
			currentSessionData = sessionData;
			binding.textViewSessionName.setText(sessionData.name());
			binding.buttonSessionCancel.setEnabled(sessionData.isCancellable());
			setError(sessionData.error());
		}

		public void resetProgress() {
			binding.progressBarSession.setProgressCompat(0, false);
			binding.progressBarSession.setMax(100);
			binding.textViewSessionPercentage.setText(itemView.getContext().getString(R.string.percentage, 0));
		}

		public void setProgress(@NonNull Progress sessionProgress, boolean animate) {
			final var progress = sessionProgress.getProgress();
			final var max = sessionProgress.getMax();
			binding.progressBarSession.setProgressCompat(progress, animate);
			binding.progressBarSession.setMax(max);
			binding.textViewSessionPercentage.setText(itemView.getContext().getString(
					R.string.percentage, (int) (((double) progress) / max * 100)));
		}

		private void setError(@NonNull NotificationString error) {
			final var fade = new Fade();
			fade.setDuration(150);
			TransitionManager.beginDelayedTransition(binding.getRoot(), fade);
			final var hasError = !error.isEmpty();
			ViewKt.setVisible(binding.textViewSessionName, !hasError);
			ViewKt.setVisible(binding.progressBarSession, !hasError);
			ViewKt.setVisible(binding.textViewSessionPercentage, !hasError);
			ViewKt.setVisible(binding.buttonSessionCancel, !hasError);
			ViewKt.setVisible(binding.textViewSessionError, hasError);
			binding.textViewSessionError.setText(error.resolve(itemView.getContext()));
		}
	}

	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
		recyclerView.setItemAnimator(ITEM_ANIMATOR);
		new ItemTouchHelper(new SwipeCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT))
				.attachToRecyclerView(recyclerView);
		isReattaching = true;
	}

	@Override
	public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
		recyclerView.setItemAnimator(new DefaultItemAnimator());
	}

	@NonNull
	@Override
	public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		final var itemBinding = ItemInstallSessionBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new SessionViewHolder(itemBinding, onCancelClick);
	}

	@Override
	public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
		onBindViewHolder(holder, position, Collections.emptyList());
	}

	@Override
	public void onBindViewHolder(@NonNull SessionViewHolder holder, int position, @NonNull List<Object> payloads) {
		final var sessionData = getItem(position);
		if (!Objects.equals(holder.getSessionId(), sessionData.id())) {
			if (currentProgress.isEmpty()) {
				holder.resetProgress();
			} else {
				final var progress = currentProgress.get(Math.min(position, currentProgress.size() - 1));
				holder.setProgress(progress.toProgress(), false);
			}
		}
		holder.bind(sessionData);
		if (!payloads.isEmpty()) {
			final var progressUpdate = (ProgressUpdate) payloads.get(payloads.size() - 1);
			holder.setProgress(progressUpdate.progress(), progressUpdate.animate());
		}
	}

	public void submitProgress(@NonNull List<SessionProgress> progress) {
		currentProgress = progress;
		if (isReattaching) {
			handler.post(() -> {
				notifyProgressChanged(progress);
				isReattaching = false;
			});
			return;
		}
		notifyProgressChanged(progress);
	}

	private void notifyProgressChanged(@NonNull List<SessionProgress> progress) {
		for (int i = 0; i < progress.size(); i++) {
			notifyItemChanged(i, new ProgressUpdate(progress.get(i).toProgress(), !isReattaching));
		}
	}

	private record ProgressUpdate(Progress progress, boolean animate) {
	}

	private static final class SessionDiffCallback extends DiffUtil.ItemCallback<SessionData> {

		@Override
		public boolean areItemsTheSame(@NonNull SessionData oldItem, @NonNull SessionData newItem) {
			return oldItem.id().equals(newItem.id());
		}

		@Override
		public boolean areContentsTheSame(@NonNull SessionData oldItem, @NonNull SessionData newItem) {
			return oldItem.equals(newItem);
		}
	}

	private static final class ItemAnimator extends DefaultItemAnimator {

		@Override
		public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
			return true;
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
			final var sessionId = Objects.requireNonNull(((SessionViewHolder) viewHolder).getSessionId(), "sessionId");
			onItemSwipe.accept(sessionId);
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