/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.sample.updater

import android.animation.LayoutTransition
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.sample.updater.databinding.ActivityMainBinding
import ru.solrudev.ackpine.session.Progress

class MainActivity : AppCompatActivity(R.layout.activity_main) {

	private val binding by viewBinding(ActivityMainBinding::bind, R.id.container_main)
	private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
		setSupportActionBar(binding.toolbarMain)
		binding.contentMain.layoutTransition?.enableTransitionType(LayoutTransition.CHANGING)
		binding.buttonMainInstall.setOnClickListener {
			viewModel.onButtonClick()
		}
		lifecycleScope.launch {
			viewModel.uiState.flowWithLifecycle(lifecycle).collect { uiState ->
				binding.cardMainInstall.containerCardInstall.isVisible = uiState.isInstallationVisible
				binding.buttonMainInstall.isEnabled = uiState.isCancellable
				binding.buttonMainInstall.text = uiState.buttonText.resolve(this@MainActivity)
				setProgress(uiState.progress)
				setError(uiState.error)
			}
		}
	}

	private fun setProgress(progressData: Progress) = with(binding) {
		val progress = progressData.progress
		val max = progressData.max
		cardMainInstall.progressBarInstall.setProgress(progress, progress != 0)
		cardMainInstall.progressBarInstall.max = max
		cardMainInstall.textViewInstallPercentage.text = getString(
			R.string.percentage,
			((progress.toDouble()) / max * 100).toInt()
		)
	}

	private fun setError(error: ResolvableString) = with(binding.cardMainInstall) {
		TransitionManager.beginDelayedTransition(root, Fade().apply { duration = 150 })
		val hasError = !error.isEmpty
		textViewInstall.isVisible = !hasError
		progressBarInstall.isVisible = !hasError
		textViewInstallPercentage.isVisible = !hasError
		textViewInstallError.isVisible = hasError
		textViewInstallError.text = error.resolve(this@MainActivity)
	}
}