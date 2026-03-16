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
package ru.solrudev.ackpine.compress.archivers.zip;

import java.io.IOException;
import java.nio.ByteBuffer;

import ru.solrudev.ackpine.compress.utils.BoundedArchiveInputStream;

/**
 * InputStream that delegates requests to the underlying SeekableByteChannel, making sure that only bytes from a certain range can be read.
 *
 * @ThreadSafe
 * @since 1.21
 */
class BoundedSeekableByteChannelInputStream extends BoundedArchiveInputStream {

	private final SeekableByteChannelCompat channel;

	/**
	 * Constructs a bounded stream on the underlying SeekableByteChannel
	 *
	 * @param start     Position in the stream from where the reading of this bounded stream starts.
	 * @param remaining Amount of bytes which are allowed to read from the bounded stream.
	 * @param channel   Channel which the reads will be delegated to.
	 */
	BoundedSeekableByteChannelInputStream(final long start, final long remaining, final SeekableByteChannelCompat channel) {
		super(start, remaining);
		this.channel = channel;
	}

	@Override
	protected int read(final long pos, final ByteBuffer buf) throws IOException {
		final int read;
		synchronized (channel) {
			channel.position(pos);
			read = channel.read(buf);
		}
		buf.flip();
		return read;
	}
}