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

package ru.solrudev.ackpine.sample.install;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipInputStream;

import ru.solrudev.ackpine.ZippedFileProvider;
import ru.solrudev.ackpine.splits.Apk;

public final class V4Signatures {

	public static final String EXTENSION = ".idsig";

	private V4Signatures() {
	}

	/**
	 * Resolves v4 signatures for the APKs being installed, keyed by {@link Apk#getName()}. Invoked off the main
	 * thread, because resolving may need to read the picked files.
	 */
	public interface Provider {

		@NonNull
		Map<String, Uri> get();
	}

	public static final Provider EMPTY = Collections::emptyMap;

	/**
	 * Reads names of the {@code .idsig} entries in the ZIP archive at {@code zipUri} and maps them to the
	 * {@link Apk#getName() names} of the APKs they belong to.
	 * <p>
	 * The APKs themselves are read by {@code ZippedApkSplits}, and the URIs it creates for them can't be reproduced
	 * here reliably. That's not a problem, because only the keys of {@code InstallParameters.getV4Signatures()} have
	 * to match the APK URIs, and they are taken from the resolved APKs. A signature's own URI just has to be
	 * readable.
	 */
	@NonNull
	public static Map<String, Uri> fromZip(@NonNull Uri zipUri, @NonNull Context context) {
		final var signatures = new HashMap<String, Uri>();
		try (final var inputStream = context.getContentResolver().openInputStream(zipUri)) {
			if (inputStream == null) {
				return signatures;
			}
			try (final var zipInputStream = new ZipInputStream(inputStream)) {
				var entry = zipInputStream.getNextEntry();
				while (entry != null) {
					final var entryName = entry.getName();
					if (!entry.isDirectory() && entryName.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
						final var apkFileName = entryName.substring(0, entryName.length() - EXTENSION.length());
						signatures.put(apkName(apkFileName), ZippedFileProvider.getUriForZipEntry(zipUri, entryName));
					}
					entry = zipInputStream.getNextEntry();
				}
			}
		} catch (IOException exception) {
			Log.e("V4Signatures", "Failed to read v4 signatures from " + zipUri, exception);
		}
		return signatures;
	}

	/**
	 * Maps the picked {@code .idsig} files to the {@link Apk#getName() names} of the APKs they belong to.
	 * <p>
	 * Signatures are paired with APKs by file name, i.e. {@code base.apk.idsig} belongs to {@code base.apk}. As a
	 * convenience, a single signature picked alongside a single APK is paired with it regardless of its file name.
	 */
	@NonNull
	public static Map<String, Uri> fromPickedFiles(@NonNull List<PickedFile> apkFiles,
												   @NonNull List<PickedFile> signatureFiles) {
		if (signatureFiles.isEmpty()) {
			return Collections.emptyMap();
		}
		if (apkFiles.size() == 1 && signatureFiles.size() == 1) {
			return Map.of(apkName(apkFiles.get(0).getName()), signatureFiles.get(0).getUri());
		}
		final var signatures = new HashMap<String, Uri>();
		for (final var file : signatureFiles) {
			final var name = file.getName();
			final var apkFileName = name.substring(0, name.length() - EXTENSION.length());
			signatures.put(apkName(apkFileName), file.getUri());
		}
		return signatures;
	}

	/**
	 * Returns the {@link Apk#getName()} for a file or ZIP entry name, which is its name without a path and an
	 * extension.
	 */
	@NonNull
	private static String apkName(@NonNull String apkFileName) {
		final var name = apkFileName.substring(apkFileName.lastIndexOf('/') + 1);
		final var extensionIndex = name.lastIndexOf('.');
		return extensionIndex != -1 ? name.substring(0, extensionIndex) : name;
	}
}
