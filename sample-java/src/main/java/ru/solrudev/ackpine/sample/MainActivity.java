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

package ru.solrudev.ackpine.sample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import ru.solrudev.ackpine.sample.databinding.NavHostBinding;

public final class MainActivity extends AppCompatActivity {

	private NavHostBinding binding;

	private final AppBarConfiguration appBarConfiguration =
			new AppBarConfiguration.Builder(R.id.install_fragment, R.id.uninstall_fragment).build();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = NavHostBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		final NavController navController = getNavController();
		NavigationUI.setupWithNavController(binding.toolbarNavHost, navController, appBarConfiguration);
		NavigationUI.setupWithNavController(binding.bottomNavigationViewNavHost, navController);
	}

	private NavController getNavController() {
		return binding.contentNavHost.<NavHostFragment>getFragment().getNavController();
	}
}