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

package ru.solrudev.ackpine.sample

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import dev.androidbroadcast.vbpd.viewBinding
import dev.chrisbanes.insetter.applyInsetter
import ru.solrudev.ackpine.sample.databinding.NavHostBinding
import ru.solrudev.ackpine.sample.install.InstallFragment

class MainActivity : AppCompatActivity(R.layout.nav_host) {

	private val binding by viewBinding(NavHostBinding::bind, R.id.container_nav_host)
	private val appBarConfiguration = AppBarConfiguration(setOf(R.id.install_fragment, R.id.uninstall_fragment))

	private val navController: NavController
		get() = binding.contentNavHost.getFragment<NavHostFragment>().navController

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
		applyInsets()
		val navController = navController
		binding.toolbarNavHost.setupWithNavController(navController, appBarConfiguration)
		binding.bottomNavigationViewNavHost.setupWithNavController(navController)
		if (savedInstanceState == null) {
			maybeHandleInstallUri(intent)
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		maybeHandleInstallUri(intent)
	}

	private fun applyInsets() = with(binding) {
		appBarLayoutNavHost.applyInsetter {
			type(statusBars = true) {
				padding()
			}
			type(navigationBars = true) {
				padding(horizontal = true)
			}
			type(displayCutout = true) {
				padding(horizontal = true, top = true)
			}
		}
		contentNavHost.applyInsetter {
			type(navigationBars = true, displayCutout = true) {
				padding(horizontal = true)
			}
		}
		bottomNavigationViewNavHost.applyInsetter {
			type(navigationBars = true, displayCutout = true) {
				padding(horizontal = true, bottom = true)
			}
		}
	}

	private fun maybeHandleInstallUri(intent: Intent) {
		val uri = intent.data
		if (intent.action == ACTION_VIEW && uri != null) {
			navController.navigate(
				R.id.install_fragment,
				bundleOf(InstallFragment.URI_KEY to uri),
				navOptions {
					launchSingleTop = true
				}
			)
		}
	}
}