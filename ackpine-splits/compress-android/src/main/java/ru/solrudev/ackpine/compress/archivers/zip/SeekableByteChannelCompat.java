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

package ru.solrudev.ackpine.compress.archivers.zip;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;

interface SeekableByteChannelCompat extends ByteChannel {

	@Override
	int read(ByteBuffer dst) throws IOException;

	@Override
	int write(ByteBuffer src) throws IOException;

	long position() throws IOException;

	SeekableByteChannelCompat position(long newPosition) throws IOException;

	long size() throws IOException;

	SeekableByteChannelCompat truncate(long size) throws IOException;

	class FileChannelWrapper implements SeekableByteChannelCompat {

		private final FileChannel delegate;

		FileChannelWrapper(FileChannel fileChannel) {
			delegate = fileChannel;
		}

		@Override
		public int read(ByteBuffer dst) throws IOException {
			return delegate.read(dst);
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			return delegate.write(src);
		}

		@Override
		public long position() throws IOException {
			return delegate.position();
		}

		@Override
		public SeekableByteChannelCompat position(long newPosition) throws IOException {
			delegate.position(newPosition);
			return this;
		}

		@Override
		public long size() throws IOException {
			return delegate.size();
		}

		@Override
		public SeekableByteChannelCompat truncate(long size) throws IOException {
			delegate.truncate(size);
			return this;
		}

		@Override
		public boolean isOpen() {
			return delegate.isOpen();
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}

		public int read(ByteBuffer dst, long position) throws IOException {
			return delegate.read(dst, position);
		}
	}

	static SeekableByteChannelCompat wrap(FileChannel fileChannel) {
		return new FileChannelWrapper(fileChannel);
	}

	@RequiresApi(Build.VERSION_CODES.N)
	static SeekableByteChannelCompat wrap(SeekableByteChannel seekableByteChannel) {
		return new SeekableByteChannelCompat() {
			@Override
			public int read(ByteBuffer dst) throws IOException {
				return seekableByteChannel.read(dst);
			}

			@Override
			public int write(ByteBuffer src) throws IOException {
				return seekableByteChannel.write(src);
			}

			@Override
			public long position() throws IOException {
				return seekableByteChannel.position();
			}

			@Override
			public SeekableByteChannelCompat position(long newPosition) throws IOException {
				seekableByteChannel.position(newPosition);
				return this;
			}

			@Override
			public long size() throws IOException {
				return seekableByteChannel.size();
			}

			@Override
			public SeekableByteChannelCompat truncate(long size) throws IOException {
				seekableByteChannel.truncate(size);
				return this;
			}

			@Override
			public boolean isOpen() {
				return seekableByteChannel.isOpen();
			}

			@Override
			public void close() throws IOException {
				seekableByteChannel.close();
			}
		};
	}
}