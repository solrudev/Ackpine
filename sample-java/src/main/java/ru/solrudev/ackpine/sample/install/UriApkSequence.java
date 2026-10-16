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

package ru.solrudev.ackpine.sample.install;

import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import ru.solrudev.ackpine.splits.Apk;
import ru.solrudev.ackpine.splits.CloseableSequence;

public final class UriApkSequence implements CloseableSequence<Apk> {

	private final List<Uri> uris;
	private final Context applicationContext;
	private final CancellationSignal cancellationSignal = new CancellationSignal();
	private volatile boolean isClosed = false;

	public UriApkSequence(@NonNull Uri uri, @NonNull Context context) {
		this(List.of(uri), context);
	}

	public UriApkSequence(@NonNull List<Uri> uris, @NonNull Context context) {
		this.uris = uris;
		applicationContext = context.getApplicationContext();
	}

	@NonNull
	@Override
	public Iterator<Apk> iterator() {
		return new Iterator<>() {

			private int index = 0;
			@Nullable
			private Apk next = null;

			@Override
			public boolean hasNext() {
				while (next == null && !isClosed && index < uris.size()) {
					next = Apk.fromUri(uris.get(index++), applicationContext, cancellationSignal);
				}
				return next != null;
			}

			@Override
			public Apk next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				final var apk = next;
				next = null;
				return apk;
			}
		};
	}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public void close() {
		isClosed = true;
		cancellationSignal.cancel();
	}
}
