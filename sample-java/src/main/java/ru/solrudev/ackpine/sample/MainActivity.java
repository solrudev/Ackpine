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

package ru.solrudev.ackpine.sample;

import static android.content.Intent.ACTION_VIEW;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import ru.solrudev.ackpine.sample.databinding.NavHostBinding;
import ru.solrudev.ackpine.sample.install.InstallFragment;

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
		if (savedInstanceState == null) {
			maybeHandleInstallUri(getIntent());
		}
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);
		maybeHandleInstallUri(intent);
	}

	private void maybeHandleInstallUri(@NonNull Intent intent) {
		final var uri = intent.getData();
		if (ACTION_VIEW.equals(intent.getAction()) && uri != null) {
			final var arguments = new Bundle();
			arguments.putParcelable(InstallFragment.URI_KEY, uri);
			final var navOptions = new NavOptions.Builder().setLaunchSingleTop(true).build();
			getNavController().navigate(R.id.install_fragment, arguments, navOptions);
		}
	}

	private NavController getNavController() {
		return binding.contentNavHost.<NavHostFragment>getFragment().getNavController();
	}
}