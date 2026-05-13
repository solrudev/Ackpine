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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DefaultLogcatExporter implements LogcatExporter {

	private static final String LOGS_DIR_NAME = "logs";
	private final Context context;

	private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
		final var thread = new Thread(runnable, "DefaultLogcatExporter");
		thread.setDaemon(true);
		return thread;
	});

	public DefaultLogcatExporter(@NonNull Context context) {
		this.context = context.getApplicationContext();
	}

	@NonNull
	@Override
	public ListenableFuture<Uri> export() {
		return Futures.submit(this::doExport, ioExecutor);
	}

	@NonNull
	private Uri doExport() throws IOException {
		final var logsDir = new File(context.getCacheDir(), LOGS_DIR_NAME);
		//noinspection ResultOfMethodCallIgnored
		logsDir.mkdirs();
		final var timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
		final var file = new File(logsDir, "ackpine-logcat-" + timestamp + ".txt");
		dumpLogcatForOwnPid(file);
		return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
	}

	private void dumpLogcatForOwnPid(@NonNull File destination) throws IOException {
		final var pid = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? "--pid=" + Process.myPid() : "";
		final var process = new ProcessBuilder("logcat", "-d", "-v", "threadtime", pid)
				.redirectErrorStream(true)
				.start();
		try {
			try (final var writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8))) {
				writer.write("Ackpine Log" + "\n");
				writer.write("Version: " + getVersionName() + "\n");
				writer.write("Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n");
				writer.write("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n");
				writer.newLine();
				try (final var reader = new BufferedReader(
						new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						writer.write(line);
						writer.newLine();
					}
				}
			}
			try {
				final var status = process.waitFor();
				if (status != 0) {
					throw new RuntimeException("logcat process exited with status " + status);
				}
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		} finally {
			process.destroy();
		}
	}

	@NonNull
	private String getVersionName() {
		try {
			final PackageInfo packageInfo;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				packageInfo = context.getPackageManager()
						.getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
			} else {
				packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			}
			return packageInfo.versionName != null ? packageInfo.versionName : "unknown";
		} catch (PackageManager.NameNotFoundException exception) {
			return "unknown";
		}
	}
}