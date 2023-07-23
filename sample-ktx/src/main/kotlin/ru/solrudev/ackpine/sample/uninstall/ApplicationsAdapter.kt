package ru.solrudev.ackpine.sample.uninstall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.sample.databinding.ItemApplicationBinding

class ApplicationsAdapter(
	private val onClick: (String) -> Unit
) : ListAdapter<ApplicationData, ApplicationsAdapter.ApplicationViewHolder>(ApplicationDiffCallback) {

	class ApplicationViewHolder(
		itemView: View,
		private val onClick: (String) -> Unit
	) : RecyclerView.ViewHolder(itemView) {

		private val binding = ItemApplicationBinding.bind(itemView)
		private var currentApplicationData: ApplicationData? = null

		init {
			binding.buttonAppUninstall.setOnClickListener {
				currentApplicationData?.let { applicationData ->
					onClick(applicationData.packageName)
				}
			}
		}

		fun bind(applicationData: ApplicationData) {
			currentApplicationData = applicationData
			binding.imageViewAppIcon.setImageDrawable(applicationData.icon)
			binding.textViewAppName.text = applicationData.name
			binding.textViewAppPackageName.text = applicationData.packageName
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_application, parent, false)
		return ApplicationViewHolder(view, onClick)
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