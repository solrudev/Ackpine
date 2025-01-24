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

import java.util.Iterator;
import java.util.NoSuchElementException;

import ru.solrudev.ackpine.splits.Apk;
import ru.solrudev.ackpine.splits.CloseableSequence;

public final class SingletonApkSequence implements CloseableSequence<Apk> {

	private final Uri uri;
	private final Context applicationContext;
	private final CancellationSignal cancellationSignal = new CancellationSignal();
	private volatile boolean isClosed = false;

	public SingletonApkSequence(@NonNull Uri uri, @NonNull Context context) {
		this.uri = uri;
		applicationContext = context.getApplicationContext();
	}

	@NonNull
	@Override
	public Iterator<Apk> iterator() {
		return new Iterator<>() {

			private final Apk apk = Apk.fromUri(uri, applicationContext, cancellationSignal);
			private boolean isYielded = false;

			@Override
			public boolean hasNext() {
				return apk != null && !isYielded;
			}

			@Override
			public Apk next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				isYielded = true;
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