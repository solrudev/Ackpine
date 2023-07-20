package ru.solrudev.ackpine.sample.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

import kotlin.sequences.Sequence;
import ru.solrudev.ackpine.splits.Apk;

public final class SingletonApkSequence implements Sequence<Apk> {

    private final Uri _uri;
    private final Context _applicationContext;

    public SingletonApkSequence(@NonNull Uri uri, @NonNull Context context) {
        _uri = uri;
        _applicationContext = context.getApplicationContext();
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
                return Apk.fromUri(_uri, _applicationContext);
            }
        };
    }
}