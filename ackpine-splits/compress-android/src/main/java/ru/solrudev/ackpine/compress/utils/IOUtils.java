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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

/**
 * Utility functions.
 *
 * @Immutable (has mutable data but it is write-only).
 */
public final class IOUtils {

	/**
	 * Internal byte array buffer, intended for both reading and writing.
	 */
	private static final ThreadLocal<byte[]> SCRATCH_BYTE_BUFFER_RW = new ThreadLocal<>();

	/**
	 * Internal byte array buffer, intended for write only operations.
	 */
	private static final byte[] SCRATCH_BYTE_BUFFER_WO = byteArray();

	/**
	 * The default buffer size ({@value}) to use in copy methods.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 8192;

	/**
	 * Represents the end-of-file (or stream) value {@value}.
	 * @since 2.5 (made public)
	 */
	public static final int EOF = -1;

	static {
		byte[] buffer = byteArray();
		Arrays.fill(buffer, (byte) 0);
		SCRATCH_BYTE_BUFFER_RW.set(buffer);
	}

	/**
	 * Gets the internal byte array intended for write only operations.
	 *
	 * @return the internal byte array intended for write only operations.
	 */
	static byte[] getScratchByteArrayWriteOnly() {
		Arrays.fill(SCRATCH_BYTE_BUFFER_WO, (byte) 0);
		return SCRATCH_BYTE_BUFFER_WO;
//		return fill0(SCRATCH_BYTE_BUFFER_WO);
	}

	/**
	 * Closes a {@link Closeable} unconditionally.
	 *
	 * <p>
	 * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored. This is typically used in
	 * finally blocks.
	 * <p>
	 * Example code:
	 * </p>
	 * <pre>
	 * Closeable closeable = null;
	 * try {
	 *     closeable = new FileReader(&quot;foo.txt&quot;);
	 *     // process closeable
	 *     closeable.close();
	 * } catch (Exception e) {
	 *     // error handling
	 * } finally {
	 *     IOUtils.closeQuietly(closeable);
	 * }
	 * </pre>
	 * <p>
	 * Closing all streams:
	 * </p>
	 * <pre>
	 * try {
	 *     return IOUtils.copy(inputStream, outputStream);
	 * } finally {
	 *     IOUtils.closeQuietly(inputStream);
	 *     IOUtils.closeQuietly(outputStream);
	 * }
	 * </pre>
	 * <p>
	 * Also consider using a try-with-resources statement where appropriate.
	 * </p>
	 *
	 * @param closeable the objects to close, may be null or already closed
	 * @since 2.0
	 * @see Throwable#addSuppressed(Throwable)
	 */
	public static void closeQuietly(final Closeable closeable) {
		try {
			closeable.close();
		} catch (final Exception e) { /* no-op */
		}
	}

	/**
	 * Reads as much from input as possible to fill the given array with the given amount of bytes.
	 * <p>
	 * This method may invoke read repeatedly to read the bytes and only read less bytes than the requested length if the end of the stream has been reached.
	 * </p>
	 *
	 * @param input  stream to read from
	 * @param array  buffer to fill
	 * @param offset offset into the buffer to start filling at
	 * @param length    of bytes to read
	 * @return the number of bytes actually read
	 * @throws IOException if an I/O error has occurred
	 */
	public static int readFully(final InputStream input, final byte[] array, final int offset, final int length) throws IOException {
		if (length < 0 || offset < 0 || length + offset > array.length || length + offset < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (length == 0) {
			return 0;
		}
		int remaining = length;
		while (remaining > 0) {
			final int location = length - remaining;
			final int count = input.read(array, offset + location, remaining);
			if (EOF == count) {
				break;
			}
			remaining -= count;
		}
		return length - remaining;
	}

	/**
	 * Reads {@code b.remaining()} bytes from the given channel starting at the current channel's position.
	 * <p>
	 * This method reads repeatedly from the channel until the requested number of bytes are read. This method blocks until the requested number of bytes are
	 * read, the end of the channel is detected, or an exception is thrown.
	 * </p>
	 *
	 * @param channel    the channel to read from
	 * @param byteBuffer the buffer into which the data is read.
	 * @throws IOException  if an I/O error occurs.
	 * @throws EOFException if the channel reaches the end before reading all the bytes.
	 */
	public static void readFully(final ReadableByteChannel channel, final ByteBuffer byteBuffer) throws IOException {
		final int length = byteBuffer.remaining();
		while (byteBuffer.remaining() > 0) {
			final int count = channel.read(byteBuffer);
			if (EOF == count) { // EOF
				break;
			}
		}
		final int read = length - byteBuffer.remaining();
		if (read < length) {
			throw new EOFException();
		}
	}

	/**
	 * Gets part of the contents of an {@code InputStream} as a {@code byte[]}.
	 *
	 * @param input the {@code InputStream} to read from
	 * @param length   maximum amount of bytes to copy
	 * @return the requested byte array
	 * @throws NullPointerException if the input is null
	 * @throws IOException          if an I/O error occurs
	 * @since 1.21
	 */
	public static byte[] readRange(final InputStream input, final int length) throws IOException {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (length == 0) {
			return output.toByteArray();
		}
		final byte[] buffer = SCRATCH_BYTE_BUFFER_RW.get();
		final int bufferLength = buffer.length;
		int bytesToRead = bufferLength;
		if (length > 0 && length < bufferLength) {
			bytesToRead = (int) length;
		}
		int read;
		long totalRead = 0;
		while (bytesToRead > 0 && EOF != (read = input.read(buffer, 0, bytesToRead))) {
			output.write(buffer, 0, read);
			totalRead += read;
			if (length > 0) { // only adjust length if not reading to the end
				// Note the cast must work because buffer.length is an integer
				bytesToRead = (int) Math.min(length - totalRead, bufferLength);
			}
		}
		return output.toByteArray();
	}

	/**
	 * Gets part of the contents of an {@code ReadableByteChannel} as a {@code byte[]}.
	 *
	 * @param input the {@code ReadableByteChannel} to read from
	 * @param length   maximum amount of bytes to copy
	 * @return the requested byte array
	 * @throws NullPointerException if the input is null
	 * @throws IOException          if an I/O error occurs
	 * @since 1.21
	 */
	public static byte[] readRange(final ReadableByteChannel input, final int length) throws IOException {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		final ByteBuffer b = ByteBuffer.allocate(Math.min(length, DEFAULT_BUFFER_SIZE));
		int read = 0;
		while (read < length) {
			// Make sure we never read more than len bytes
			b.limit(Math.min(length - read, b.capacity()));
			final int readCount = input.read(b);
			if (readCount <= 0) {
				break;
			}
			output.write(b.array(), 0, readCount);
			b.rewind();
			read += readCount;
		}
		return output.toByteArray();
	}

	/**
	 * Skips bytes from an input byte stream.
	 * This implementation guarantees that it will read as many bytes
	 * as possible before giving up; this may not always be the case for
	 * skip() implementations in subclasses of {@link InputStream}.
	 * <p>
	 * Note that the implementation uses {@link InputStream#read(byte[], int, int)} rather
	 * than delegating to {@link InputStream#skip(long)}.
	 * This means that the method may be considerably less efficient than using the actual skip implementation,
	 * this is done to guarantee that the correct number of bytes are skipped.
	 * </p>
	 *
	 * @param input byte stream to skip
	 * @param skip number of bytes to skip.
	 * @return number of bytes actually skipped.
	 * @throws IOException              if there is a problem reading the file
	 * @throws IllegalArgumentException if toSkip is negative
	 * @see InputStream#skip(long)
	 * @see <a href="https://issues.apache.org/jira/browse/IO-203">IO-203 - Add skipFully() method for InputStreams</a>
	 * @since 2.0
	 */
	public static long skip(final InputStream input, final long skip) throws IOException {
		if (skip < 0) {
			throw new IllegalArgumentException("Skip count must be non-negative, actual: " + skip);
		}
		//
		// No need to synchronize access to SCRATCH_BYTE_BUFFER_WO: We don't care if the buffer is written multiple
		// times or in parallel since the data is ignored. We reuse the same buffer, if the buffer size were variable or read-write,
		// we would need to synch or use a thread local to ensure some other thread safety.
		//
		long remain = skip;
		while (remain > 0) {
			final byte[] skipBuffer = getScratchByteArrayWriteOnly();
//			final byte[] skipBuffer = skipBufferSupplier.get();
			// See https://issues.apache.org/jira/browse/IO-203 for why we use read() rather than delegating to skip()
			final long n = input.read(skipBuffer, 0, (int) Math.min(remain, skipBuffer.length));
			if (n < 0) { // EOF
				break;
			}
			remain -= n;
		}
		return skip - remain;
	}

	private static byte[] byteArray() {
		return new byte[DEFAULT_BUFFER_SIZE];
	}

	/** Private constructor to prevent instantiation of this utility class. */
	private IOUtils() {
	}

}
