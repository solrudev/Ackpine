/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.sample.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.solrudev.ackpine.sample.settings.TestSettingsRepository.createSettingsRepository;

import android.net.Uri;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import ru.solrudev.ackpine.test.futures.ImmediateFuture;

public class SettingsViewModelTest {

	@Rule
	public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

	@Test
	public void exportLogsExposesSuccessEventOnLiveData() {
		final LogcatExporter exporter = () -> ImmediateFuture.success(Uri.EMPTY);
		final var viewModel = new SettingsViewModel(createSettingsRepository(), exporter);

		viewModel.exportLogs();

		assertEquals(new LogcatExportEvent.Success(Uri.EMPTY), viewModel.getLogcatExportEvent().getValue());
	}

	@Test
	public void exportLogsExposesFailureEventOnLiveDataWhenExporterThrows() {
		final LogcatExporter exporter = () -> ImmediateFuture.failure(new IOException("boom"));
		final var viewModel = new SettingsViewModel(createSettingsRepository(), exporter);

		viewModel.exportLogs();

		assertEquals(LogcatExportEvent.Failure.INSTANCE, viewModel.getLogcatExportEvent().getValue());
	}

	@Test
	public void consumeLogcatExportEventClearsItFromLiveData() {
		final LogcatExporter exporter = () -> ImmediateFuture.success(Uri.EMPTY);
		final var viewModel = new SettingsViewModel(createSettingsRepository(), exporter);

		viewModel.exportLogs();
		assertEquals(new LogcatExportEvent.Success(Uri.EMPTY), viewModel.getLogcatExportEvent().getValue());

		viewModel.consumeLogcatExportEvent();
		assertNull(viewModel.getLogcatExportEvent().getValue());
	}
}