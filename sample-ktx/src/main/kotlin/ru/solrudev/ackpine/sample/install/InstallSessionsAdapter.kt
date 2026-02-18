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

package ru.solrudev.ackpine.sample.install

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.sample.databinding.ItemInstallSessionBinding
import ru.solrudev.ackpine.session.Progress
import java.util.UUID

class InstallSessionsAdapter(
	private val onCancelClick: (UUID) -> Unit,
	private val onItemSwipe: (UUID) -> Unit
) : ListAdapter<SessionData, InstallSessionsAdapter.SessionViewHolder>(SessionDiffCallback) {

	private val handler = Handler(Looper.getMainLooper())
	private var isReattaching = false
	private var currentProgress = emptyList<SessionProgress>()

	class SessionViewHolder(
		private val itemBinding: ItemInstallSessionBinding,
		private val onClick: (UUID) -> Unit
	) : RecyclerView.ViewHolder(itemBinding.root) {

		private var currentSessionData: SessionData? = null

		init {
			itemBinding.buttonSessionCancel.setOnClickListener {
				currentSessionData?.let { sessionData ->
					onClick(sessionData.id)
				}
			}
		}

		val isSwipeable: Boolean
			get() = currentSessionData?.error?.isEmpty == false

		val sessionId: UUID?
			get() = currentSessionData?.id

		fun bind(sessionData: SessionData) = with(itemBinding) {
			currentSessionData = sessionData
			textViewSessionName.text = sessionData.name
			buttonSessionCancel.isEnabled = sessionData.isCancellable
			setError(sessionData.error)
		}

		fun resetProgress() = with(itemBinding) {
			progressBarSession.setProgressCompat(0, false)
			progressBarSession.max = 100
			textViewSessionPercentage.text = itemView.context.getString(R.string.percentage, 0)
		}

		fun setProgress(sessionProgress: Progress, animate: Boolean) = with(itemBinding) {
			val progress = sessionProgress.progress
			val max = sessionProgress.max
			progressBarSession.setProgressCompat(progress, animate)
			progressBarSession.max = max
			textViewSessionPercentage.text = itemView.context.getString(
				R.string.percentage, (progress.toDouble() / max * 100).toInt()
			)
		}

		private fun setError(error: ResolvableString) = with(itemBinding) {
			TransitionManager.beginDelayedTransition(root, Fade().apply { duration = 150 })
			val hasError = !error.isEmpty
			textViewSessionName.isVisible = !hasError
			progressBarSession.isVisible = !hasError
			textViewSessionPercentage.isVisible = !hasError
			buttonSessionCancel.isVisible = !hasError
			textViewSessionError.isVisible = hasError
			textViewSessionError.text = error.resolve(itemView.context)
		}
	}

	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		recyclerView.itemAnimator = ItemAnimator
		ItemTouchHelper(SwipeCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT))
			.attachToRecyclerView(recyclerView)
		isReattaching = true
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
		val itemBinding = ItemInstallSessionBinding.inflate(
			LayoutInflater.from(parent.context), parent, false
		)
		return SessionViewHolder(itemBinding, onCancelClick)
	}

	override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
		onBindViewHolder(holder, position, emptyList())
	}

	override fun onBindViewHolder(holder: SessionViewHolder, position: Int, payloads: List<Any>) {
		val sessionData = getItem(position)
		if (holder.sessionId != sessionData.id) {
			if (currentProgress.isEmpty()) {
				holder.resetProgress()
			} else {
				val progress = currentProgress[position.coerceAtMost(currentProgress.size - 1)]
				holder.setProgress(progress.progress, animate = false)
			}
		}
		holder.bind(sessionData)
		if (payloads.isNotEmpty()) {
			val progressUpdate = payloads.last() as ProgressUpdate
			holder.setProgress(progressUpdate.progress, progressUpdate.animate)
		}
	}

	fun submitProgress(progress: List<SessionProgress>) {
		val oldProgress = currentProgress
		currentProgress = progress
		if (isReattaching) {
			handler.post {
				notifyProgressChanged(oldProgress, progress)
				isReattaching = false
			}
			return
		}
		notifyProgressChanged(oldProgress, progress)
	}

	private fun notifyProgressChanged(oldProgress: List<SessionProgress>, progress: List<SessionProgress>) {
		if (oldProgress.size == progress.size) {
			progress.forEachIndexed { index, sessionProgress ->
				val newProgress = sessionProgress.progress
				if (newProgress != oldProgress[index].progress) {
					notifyItemChanged(index, ProgressUpdate(newProgress, !isReattaching))
				}
			}
			return
		}
		val oldProgressById = oldProgress.associateBy(
			keySelector = { sessionProgress -> sessionProgress.id },
			valueTransform = { sessionProgress -> sessionProgress.progress }
		)
		progress.forEachIndexed { index, sessionProgress ->
			val newProgress = sessionProgress.progress
			if (newProgress != oldProgressById[sessionProgress.id]) {
				notifyItemChanged(index, ProgressUpdate(newProgress, !isReattaching))
			}
		}
	}

	private data class ProgressUpdate(val progress: Progress, val animate: Boolean)

	private object SessionDiffCallback : DiffUtil.ItemCallback<SessionData>() {

		override fun areItemsTheSame(oldItem: SessionData, newItem: SessionData): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: SessionData, newItem: SessionData): Boolean {
			return oldItem == newItem
		}
	}

	private object ItemAnimator : DefaultItemAnimator() {
		override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder) = true
	}

	private inner class SwipeCallback(
		dragDirs: Int,
		swipeDirs: Int
	) : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		) = false

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			val sessionId = checkNotNull((viewHolder as SessionViewHolder).sessionId) { "sessionId" }
			onItemSwipe(sessionId)
		}

		override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
			if ((viewHolder as SessionViewHolder).isSwipeable) {
				return super.getSwipeDirs(recyclerView, viewHolder)
			}
			return 0
		}
	}
}