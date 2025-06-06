/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.solrudev.ackpine.compress.utils;

import static ru.solrudev.ackpine.compress.utils.IOUtils.EOF;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Always returns {@link IOUtils#EOF} to all attempts to read something from the stream.
 * <p>
 * Typically uses of this class include testing for corner cases in methods that accept input streams and acting as a
 * sentinel value instead of a {@code null} input stream.
 * </p>
 *
 * @since 1.4
 */
public class ClosedInputStream extends InputStream {

	/**
	 * The singleton instance.
	 *
	 * @since 2.12.0
	 */
	public static final ClosedInputStream INSTANCE = new ClosedInputStream();

	/**
	 * The singleton instance.
	 *
	 * @deprecated Use {@link #INSTANCE}.
	 */
	@Deprecated
	public static final ClosedInputStream CLOSED_INPUT_STREAM = INSTANCE;

	/**
	 * Returns -1 to indicate that the stream is closed.
	 *
	 * @return always -1
	 */
	@Override
	public int read() {
		return EOF;
	}

	/**
	 * Returns -1 to indicate that the stream is closed.
	 *
	 * @param b ignored.
	 * @param off ignored.
	 * @param len ignored.
	 * @return always -1
	 */
	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		return EOF;
	}

}
