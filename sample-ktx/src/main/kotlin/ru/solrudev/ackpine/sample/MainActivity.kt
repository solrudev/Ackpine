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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import ru.solrudev.ackpine.sample.databinding.NavHostBinding

class MainActivity : AppCompatActivity(R.layout.nav_host) {

	private val binding by viewBinding(NavHostBinding::bind, R.id.container_nav_host)
	private val appBarConfiguration = AppBarConfiguration(setOf(R.id.install_fragment, R.id.uninstall_fragment))

	private val navController: NavController
		get() = binding.contentNavHost.getFragment<NavHostFragment>().navController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
		val navController = navController
		binding.toolbarNavHost.setupWithNavController(navController, appBarConfiguration)
		binding.bottomNavigationViewNavHost.setupWithNavController(navController)
	}
}