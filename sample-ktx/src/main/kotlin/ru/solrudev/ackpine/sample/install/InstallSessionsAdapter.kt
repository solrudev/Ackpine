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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.sample.databinding.ItemInstallSessionBinding
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.parameters.NotificationString
import java.util.UUID

class InstallSessionsAdapter(
	private val onClick: (UUID) -> Unit
) : ListAdapter<SessionData, InstallSessionsAdapter.SessionViewHolder>(SessionDiffCallback) {

	class SessionViewHolder(
		itemView: View,
		private val onClick: (UUID) -> Unit
	) : RecyclerView.ViewHolder(itemView) {

		private val binding = ItemInstallSessionBinding.bind(itemView)
		private var currentSessionData: SessionData? = null

		init {
			binding.buttonSessionCancel.setOnClickListener {
				currentSessionData?.let { sessionData ->
					onClick(sessionData.id)
				}
			}
		}

		val isSwipeable: Boolean
			get() = currentSessionData?.error?.isEmpty?.not() ?: false

		val sessionId: UUID?
			get() = currentSessionData?.id

		fun bind(sessionData: SessionData) = with(binding) {
			currentSessionData = sessionData
			progressBarSession.setProgressCompat(0, false)
			textViewSessionName.text = sessionData.name
			setError(sessionData.error)
		}

		fun setProgress(sessionProgress: Progress) = with(binding) {
			val progress = sessionProgress.progress
			val max = sessionProgress.max
			progressBarSession.setProgressCompat(progress, true)
			progressBarSession.max = max
			textViewSessionPercentage.text = itemView.context.getString(
				R.string.percentage, (progress.toDouble() / max * 100).toInt()
			)
		}

		private fun setError(error: NotificationString) = with(binding) {
			val hasError = !error.isEmpty
			textViewSessionName.isVisible = !hasError
			progressBarSession.isVisible = !hasError
			textViewSessionPercentage.isVisible = !hasError
			buttonSessionCancel.isVisible = !hasError
			textViewSessionError.isVisible = hasError
			textViewSessionError.text = error.resolve(itemView.context)
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_install_session, parent, false)
		return SessionViewHolder(view, onClick)
	}

	override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
		onBindViewHolder(holder, position, emptyList())
	}

	override fun onBindViewHolder(holder: SessionViewHolder, position: Int, payloads: List<Any>) {
		val sessionData = getItem(position)
		if (payloads.isEmpty()) {
			holder.bind(sessionData)
		} else {
			holder.setProgress(payloads.first() as Progress)
		}
	}

	fun submitProgress(progress: List<SessionProgress>) {
		progress.forEachIndexed { index, sessionProgress ->
			notifyItemChanged(index, sessionProgress.progress)
		}
	}

	private object SessionDiffCallback : DiffUtil.ItemCallback<SessionData>() {

		override fun areItemsTheSame(oldItem: SessionData, newItem: SessionData): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: SessionData, newItem: SessionData): Boolean {
			return oldItem == newItem
		}
	}
}