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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.util.Locale;
import java.util.Set;

/**
 * A file picked by the user, classified by its extension, falling back to its MIME type.
 */
public final class PickedFile {

	public static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
	public static final String ZIP_MIME_TYPE = "application/zip";
	public static final String BINARY_MIME_TYPE = "application/octet-stream";

	private static final Set<String> ARCHIVE_TYPES =
			Set.of("zip", "apks", "xapk", "apkm", ZIP_MIME_TYPE, BINARY_MIME_TYPE);

	private final Uri uri;
	private final String name;
	@Nullable
	private final String fileType;

	private PickedFile(@NonNull Uri uri, @NonNull String name, @Nullable String fileType) {
		this.uri = uri;
		this.name = name;
		this.fileType = fileType;
	}

	@NonNull
	public static PickedFile from(@NonNull Uri uri, @NonNull Context context) {
		final var document = DocumentFile.fromSingleUri(context, uri);
		final var documentName = document != null ? document.getName() : null;
		final var name = documentName != null ? documentName : "";
		final var extensionIndex = name.lastIndexOf('.') + 1;
		var fileType = extensionIndex != 0 ? name.substring(extensionIndex) : "";
		if (fileType.isEmpty()) {
			final var mimeType = context.getContentResolver().getType(uri);
			fileType = mimeType != null ? mimeType : "";
		}
		return new PickedFile(uri, name, fileType.isEmpty() ? null : fileType.toLowerCase(Locale.ROOT));
	}

	@NonNull
	public Uri getUri() {
		return uri;
	}

	@NonNull
	public String getName() {
		return name;
	}

	public boolean isArchive() {
		return fileType != null && ARCHIVE_TYPES.contains(fileType);
	}

	/**
	 * A v4 signature is recognized by its extension only: it has no MIME type of its own, and document providers
	 * report it as {@link #BINARY_MIME_TYPE}, which is also used for archives.
	 */
	public boolean isV4Signature() {
		return name.toLowerCase(Locale.ROOT).endsWith(V4Signatures.EXTENSION);
	}
}
