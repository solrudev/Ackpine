/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package ru.solrudev.ackpine.compress.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

//@formatter:off
/**
 * Reads bytes up to a maximum count and stops once reached.
 * <p>
 * To build an instance: Use the {@link #builder()} to access all features.
 * </p>
 * <p>
 * By default, a {@link BoundedInputStream} is <em>unbound</em>; so make sure to call {@link BoundedInputStream.Builder#setMaxCount(long)}.
 * </p>
 * <p>
 * You can find out how many bytes this stream has seen so far by calling {@link BoundedInputStream#getCount()}. This value reflects bytes read and skipped.
 * </p>
 * <h2>Using a ServletInputStream</h2>
 * <p>
 * A {@code ServletInputStream} can block if you try to read content that isn't there
 * because it doesn't know whether the content hasn't arrived yet or whether the content has finished. Initialize an {@link BoundedInputStream} with the
 * {@code Content-Length} sent in the {@code ServletInputStream}'s header, this stop it from blocking, providing it's been sent with a correct content
 * length in the first place.
 * </p>
 * <h2>Using NIO</h2>
 * <pre>{@code
 * BoundedInputStream s = BoundedInputStream.builder()
 *   .setPath(Paths.get("MyFile.xml"))
 *   .setMaxCount(1024)
 *   .setPropagateClose(false)
 *   .get();
 * }
 * </pre>
 * <h2>Using IO</h2>
 * <pre>{@code
 * BoundedInputStream s = BoundedInputStream.builder()
 *   .setFile(new File("MyFile.xml"))
 *   .setMaxCount(1024)
 *   .setPropagateClose(false)
 *   .get();
 * }
 * </pre>
 * <h2>Counting Bytes</h2>
 * <p>You can set the running count when building, which is most useful when starting from another stream:
 * <pre>{@code
 * InputStream in = ...;
 * BoundedInputStream s = BoundedInputStream.builder()
 *   .setInputStream(in)
 *   .setCount(12)
 *   .setMaxCount(1024)
 *   .setPropagateClose(false)
 *   .get();
 * }
 * </pre>
 * <h2>Listening for the max count reached</h2>
 * <pre>{@code
 * BoundedInputStream s = BoundedInputStream.builder()
 *   .setPath(Paths.get("MyFile.xml"))
 *   .setMaxCount(1024)
 *   .setOnMaxCount((max, count) -> System.out.printf("Max count %,d reached with a last read count of %,d%n", max, count))
 *   .get();
 * }
 * </pre>
 * @see BoundedInputStream.Builder
 * @since 2.0
 */
//@formatter:on
public class BoundedInputStream extends FilterInputStream {

	//@formatter:off
	/**
	 * Builds a new {@link BoundedInputStream}.
	 * <p>
	 * By default, a {@link BoundedInputStream} is <em>unbound</em>; so make sure to call {@link BoundedInputStream.Builder#setMaxCount(long)}.
	 * </p>
	 * <p>
	 * You can find out how many bytes this stream has seen so far by calling {@link BoundedInputStream#getCount()}. This value reflects bytes read and skipped.
	 * </p>
	 * <h2>Using a ServletInputStream</h2>
	 * <p>
	 * A {@code ServletInputStream} can block if you try to read content that isn't there
	 * because it doesn't know whether the content hasn't arrived yet or whether the content has finished. Initialize an {@link BoundedInputStream} with the
	 * {@code Content-Length} sent in the {@code ServletInputStream}'s header, this stop it from blocking, providing it's been sent with a correct content
	 * length in the first place.
	 * </p>
	 * <h2>Using NIO</h2>
	 * <pre>{@code
	 * BoundedInputStream s = BoundedInputStream.builder()
	 *   .setPath(Paths.get("MyFile.xml"))
	 *   .setMaxCount(1024)
	 *   .setPropagateClose(false)
	 *   .get();
	 * }
	 * </pre>
	 * <h2>Using IO</h2>
	 * <pre>{@code
	 * BoundedInputStream s = BoundedInputStream.builder()
	 *   .setFile(new File("MyFile.xml"))
	 *   .setMaxCount(1024)
	 *   .setPropagateClose(false)
	 *   .get();
	 * }
	 * </pre>
	 * <h2>Counting Bytes</h2>
	 * <p>You can set the running count when building, which is most useful when starting from another stream:
	 * <pre>{@code
	 * InputStream in = ...;
	 * BoundedInputStream s = BoundedInputStream.builder()
	 *   .setInputStream(in)
	 *   .setCount(12)
	 *   .setMaxCount(1024)
	 *   .setPropagateClose(false)
	 *   .get();
	 * }
	 * </pre>
	 *
	 * @see #get()
	 * @since 2.16.0
	 */
	//@formatter:on
	public static class Builder {

		private InputStream origin;
		private long maxCount = EOF;

		/**
		 * Constructs a new builder of {@link BoundedInputStream}.
		 */
		public Builder() {
			// empty
		}

		/**
		 * Sets a new origin.
		 *
		 * @param origin the new origin.
		 * @return {@code this} instance.
		 */
		public Builder setInputStream(final InputStream origin) {
			this.origin = origin;
			return this;
		}

		public InputStream getInputStream() {
			if (origin == null) {
				throw new IllegalStateException("origin == null");
			}
			return origin;
		}

		public long getMaxCount() {
			return maxCount;
		}

		public Builder setMaxCount(long maxCount) {
			this.maxCount = maxCount;
			return this;
		}

		/**
		 * Builds a new {@link BoundedInputStream}.
		 * <p>
		 * You must set an aspect that supports {@link #getInputStream()}, otherwise, this method throws an exception.
		 * </p>
		 * <p>
		 * This builder uses the following aspects:
		 * </p>
		 * <ul>
		 * <li>{@link #getInputStream()} gets the target aspect.</li>
		 * <li>{@link #getCount()}</li>
		 * <li>{@link #getMaxCount()}</li>
		 * </ul>
		 *
		 * @return a new instance.
		 * @throws IllegalStateException         if the {@code origin} is {@code null}.
		 * @see #getInputStream()
		 */
		public BoundedInputStream get() throws IOException {
			return new BoundedInputStream(this);
		}
	}

	private static final int EOF = -1;

	/**
	 * Constructs a new {@link BoundedInputStream.Builder}.
	 *
	 * @return a new {@link BoundedInputStream.Builder}.
	 * @since 2.16.0
	 */
	public static BoundedInputStream.Builder builder() {
		return new BoundedInputStream.Builder();
	}

	/** The current count of bytes counted. */
	private long count;

	/** The current mark. */
	private long mark;

	/** The max count of bytes to read. */
	private final long maxCount;

	BoundedInputStream(final BoundedInputStream.Builder builder) {
		super(builder.getInputStream());
		this.count = 0;
		this.maxCount = builder.getMaxCount();
	}

	/**
	 * Constructs a new {@link BoundedInputStream} that wraps the given input stream and is <em>unbounded</em>.
	 * <p>
	 * To build an instance: Use the {@link #builder()} to access all features.
	 * </p>
	 *
	 * @param in The wrapped input stream.
	 * @deprecated Use {@link BoundedInputStream.Builder#get()}.
	 */
	@Deprecated
	public BoundedInputStream(final InputStream in) {
		this(builder().setInputStream(in));
	}

	/**
	 * Constructs a new {@link BoundedInputStream} that wraps the given input stream and limits it to a certain size.
	 *
	 * @param inputStream The wrapped input stream.
	 * @param maxCount    The maximum number of bytes to return, negative means unbound.
	 * @deprecated Use {@link BoundedInputStream.Builder#get()}.
	 */
	@Deprecated
	public BoundedInputStream(final InputStream inputStream, final long maxCount) {
		// Some badly designed methods - e.g. the Servlet API - overload length
		// such that "-1" means stream finished
		this(builder().setInputStream(inputStream).setMaxCount(maxCount));
	}

	/**
	 * Adds the number of read bytes to the count.
	 *
	 * @param n number of bytes read, or -1 if no more bytes are available
	 * @throws IOException Not thrown here but subclasses may throw.
	 * @since 2.0
	 */
	private synchronized void afterRead(final int n) throws IOException {
		if (n != EOF) {
			count += n;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int available() throws IOException {
		if (isMaxCount()) {
			return 0;
		}
		return in.available();
	}

	/**
	 * Invokes the delegate's {@link InputStream#close()} method.
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		try {
			in.close();
		} catch (final IOException e) {
			rethrow(e);
		} catch (final Exception e) {
			rethrow(new IOException(e));
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void rethrow(final Throwable throwable) throws T {
		throw (T) throwable;
	}

	/**
	 * Gets the count of bytes read.
	 *
	 * @return The count of bytes read.
	 * @since 2.12.0
	 */
	public synchronized long getCount() {
		return count;
	}

	/**
	 * Gets the max count of bytes to read.
	 *
	 * @return The max count of bytes to read.
	 * @since 2.16.0
	 */
	public long getMaxCount() {
		return maxCount;
	}

	/**
	 * Gets the max count of bytes to read.
	 *
	 * @return The max count of bytes to read.
	 * @since 2.12.0
	 * @deprecated Use {@link #getMaxCount()}.
	 */
	@Deprecated
	public long getMaxLength() {
		return maxCount;
	}

	/**
	 * Gets how many bytes remain to read.
	 *
	 * @return bytes how many bytes remain to read.
	 * @since 2.16.0
	 */
	public long getRemaining() {
		return Math.max(0, getMaxCount() - getCount());
	}

	private boolean isMaxCount() {
		return maxCount >= 0 && getCount() >= maxCount;
	}

	/**
	 * Invokes the delegate's {@link InputStream#mark(int)} method.
	 *
	 * @param readLimit read ahead limit
	 */
	@Override
	public synchronized void mark(final int readLimit) {
		in.mark(readLimit);
		mark = count;
	}

	/**
	 * Invokes the delegate's {@link InputStream#markSupported()} method.
	 *
	 * @return true if mark is supported, otherwise false
	 */
	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	/**
	 * Invokes the delegate's {@link InputStream#read()} method if the current position is less than the limit.
	 *
	 * @return the byte read or -1 if the end of stream or the limit has been reached.
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public int read() throws IOException {
		if (isMaxCount()) {
			return EOF;
		}
		try {
			final int b = in.read();
			afterRead(b != EOF ? 1 : EOF);
			return b;
		} catch (final IOException e) {
			rethrow(e);
			return EOF;
		}
	}

	/**
	 * Invokes the delegate's {@link InputStream#read(byte[])} method.
	 *
	 * @param b the buffer to read the bytes into
	 * @return the number of bytes read or -1 if the end of stream or the limit has been reached.
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public int read(final byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * Invokes the delegate's {@link InputStream#read(byte[], int, int)} method.
	 *
	 * @param b   the buffer to read the bytes into
	 * @param off The start offset
	 * @param len The number of bytes to read
	 * @return the number of bytes read or -1 if the end of stream or the limit has been reached.
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		if (isMaxCount()) {
			return EOF;
		}
		try {
			final int n = in.read(b, off, (int) toReadLen(len));
			afterRead(n);
			return n;
		} catch (final IOException e) {
			rethrow(e);
			return EOF;
		}
	}

	/**
	 * Invokes the delegate's {@link InputStream#reset()} method.
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public synchronized void reset() throws IOException {
		in.reset();
		count = mark;
	}

	/**
	 * Invokes the delegate's {@link InputStream#skip(long)} method.
	 *
	 * @param n the number of bytes to skip
	 * @return the actual number of bytes skipped
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public synchronized long skip(final long n) throws IOException {
		long skip = 0;
		try {
			skip = in.skip(toReadLen(n));
		} catch (final IOException e) {
			rethrow(e);
		}
		count += skip;
		return skip;
	}

	private long toReadLen(final long len) {
		return maxCount >= 0 ? Math.min(len, maxCount - getCount()) : len;
	}

	/**
	 * Invokes the delegate's {@link InputStream#toString()} method.
	 *
	 * @return the delegate's {@link InputStream#toString()}
	 */
	@Override
	public String toString() {
		return in.toString();
	}
}
