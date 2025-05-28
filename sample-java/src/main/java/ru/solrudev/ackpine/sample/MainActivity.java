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

import static android.content.Intent.ACTION_VIEW;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import dev.chrisbanes.insetter.Insetter;
import dev.chrisbanes.insetter.Side;
import ru.solrudev.ackpine.sample.databinding.NavHostBinding;
import ru.solrudev.ackpine.sample.install.InstallFragment;

public final class MainActivity extends AppCompatActivity {

	private NavHostBinding binding;

	private final AppBarConfiguration appBarConfiguration =
			new AppBarConfiguration.Builder(R.id.install_fragment, R.id.uninstall_fragment).build();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		EdgeToEdge.enable(this);
		super.onCreate(savedInstanceState);
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
			getIntent().setData(null);
		}
		binding = NavHostBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		applyInsets();
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

	private void applyInsets() {
		Insetter.builder()
				.padding(WindowInsetsCompat.Type.statusBars())
				.padding(WindowInsetsCompat.Type.navigationBars(), Side.LEFT | Side.RIGHT)
				.padding(WindowInsetsCompat.Type.displayCutout(), Side.LEFT | Side.RIGHT | Side.TOP)
				.applyToView(binding.appBarLayoutNavHost);
		Insetter.builder()
				.padding(WindowInsetsCompat.Type.navigationBars(), Side.LEFT | Side.RIGHT)
				.padding(WindowInsetsCompat.Type.displayCutout(), Side.LEFT | Side.RIGHT)
				.applyToView(binding.contentNavHost);
		Insetter.builder()
				.padding(WindowInsetsCompat.Type.navigationBars(), Side.LEFT | Side.RIGHT | Side.BOTTOM)
				.padding(WindowInsetsCompat.Type.displayCutout(), Side.LEFT | Side.RIGHT | Side.BOTTOM)
				.applyToView(binding.bottomNavigationViewNavHost);
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