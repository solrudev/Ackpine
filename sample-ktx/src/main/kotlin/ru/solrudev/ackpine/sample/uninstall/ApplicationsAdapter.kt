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

package ru.solrudev.ackpine.sample.uninstall

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.solrudev.ackpine.sample.databinding.ItemApplicationBinding

class ApplicationsAdapter(
	private val onClick: (String) -> Unit
) : ListAdapter<ApplicationData, ApplicationsAdapter.ApplicationViewHolder>(ApplicationDiffCallback) {

	class ApplicationViewHolder(
		private val itemBinding: ItemApplicationBinding,
		private val onClick: (String) -> Unit
	) : RecyclerView.ViewHolder(itemBinding.root) {

		private var currentApplicationData: ApplicationData? = null

		init {
			itemBinding.buttonAppUninstall.setOnClickListener {
				currentApplicationData?.let { applicationData ->
					onClick(applicationData.packageName)
				}
			}
		}

		fun bind(applicationData: ApplicationData) = with(itemBinding) {
			currentApplicationData = applicationData
			imageViewAppIcon.setImageDrawable(applicationData.icon)
			textViewAppName.text = applicationData.name
			textViewAppPackageName.text = applicationData.packageName
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
		val itemBinding = ItemApplicationBinding.inflate(
			LayoutInflater.from(parent.context), parent, false
		)
		return ApplicationViewHolder(itemBinding, onClick)
	}

	override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
		val applicationData = getItem(position)
		holder.bind(applicationData)
	}

	private object ApplicationDiffCallback : DiffUtil.ItemCallback<ApplicationData?>() {

		override fun areItemsTheSame(oldItem: ApplicationData, newItem: ApplicationData): Boolean {
			return oldItem.packageName == newItem.packageName
		}

		override fun areContentsTheSame(oldItem: ApplicationData, newItem: ApplicationData): Boolean {
			return oldItem == newItem
		}
	}
}