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

package ru.solrudev.ackpine.sample.install

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.sample.databinding.ItemInstallSessionBinding
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.parameters.NotificationString
import java.util.UUID

class InstallSessionsAdapter(
	private val onClick: (UUID) -> Unit
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
			get() = currentSessionData?.error?.isEmpty?.not() ?: false

		val sessionId: UUID
			get() = requireNotNull(currentSessionData?.id) { "currentSessionData" }

		fun bind(sessionData: SessionData) = with(itemBinding) {
			if (currentSessionData?.id != sessionData.id) {
				progressBarSession.setProgressCompat(0, false)
			}
			currentSessionData = sessionData
			textViewSessionName.text = sessionData.name
			buttonSessionCancel.isEnabled = sessionData.isCancellable
			setError(sessionData.error)
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

		private fun setError(error: NotificationString) = with(itemBinding) {
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
		isReattaching = true
	}

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		recyclerView.itemAnimator = DefaultItemAnimator()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
		val itemBinding = ItemInstallSessionBinding.inflate(
			LayoutInflater.from(parent.context), parent, false
		)
		return SessionViewHolder(itemBinding, onClick)
	}

	override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
		onBindViewHolder(holder, position, emptyList())
	}

	override fun onBindViewHolder(holder: SessionViewHolder, position: Int, payloads: List<Any>) {
		val sessionData = getItem(position)
		holder.bind(sessionData)
		if (payloads.isEmpty()) {
			val progress = currentProgress[position]
			holder.setProgress(progress.progress, animate = false)
		} else {
			val progressUpdate = payloads.first() as ProgressUpdate
			holder.setProgress(progressUpdate.progress, progressUpdate.animate)
		}
	}

	fun submitProgress(progress: List<SessionProgress>) {
		currentProgress = progress
		if (isReattaching) {
			handler.post {
				notifyProgressChanged(progress)
				isReattaching = false
			}
			return
		}
		notifyProgressChanged(progress)
	}

	private fun notifyProgressChanged(progress: List<SessionProgress>) {
		progress.forEachIndexed { index, sessionProgress ->
			notifyItemChanged(index, ProgressUpdate(sessionProgress.progress, !isReattaching))
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
}