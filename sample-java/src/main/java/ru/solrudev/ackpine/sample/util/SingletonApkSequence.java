package ru.solrudev.ackpine.sample.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

import kotlin.sequences.Sequence;
import ru.solrudev.ackpine.splits.Apk;

public final class SingletonApkSequence implements Sequence<Apk> {

	private final Uri uri;
	private final Context applicationContext;

	public SingletonApkSequence(@NonNull Uri uri, @NonNull Context context) {
		this.uri = uri;
		applicationContext = context.getApplicationContext();
	}

	@NonNull
	@Override
	public Iterator<Apk> iterator() {
		return new Iterator<>() {

			private boolean isYielded = false;

			@Override
			public boolean hasNext() {
				return !isYielded;
			}

			@Override
			public Apk next() {
				if (isYielded) {
					throw new NoSuchElementException();
				}
				isYielded = true;
				return Apk.fromUri(uri, applicationContext);
			}
		};
	}
}