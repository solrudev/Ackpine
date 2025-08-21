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
package ru.solrudev.ackpine.compress;

import java.io.IOException;

/**
 * Signals that a Pack200 Compress exception of some sort has occurred.
 *
 * @since 1.28.0
 */
public class CompressException extends IOException {

	/** Serial. */
	private static final long serialVersionUID = 1;

	/**
	 * Constructs an {@code CompressException} with {@code null} as its error detail message.
	 */
	public CompressException() {
		// empty
	}

	/**
	 * Constructs a new exception with the specified detail message. The cause is not initialized.
	 *
	 * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method).
	 */
	public CompressException(final String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method).
	 * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method). A null value indicates that the cause is nonexistent or
	 *                unknown.
	 */
	public CompressException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a {@code CompressException} with the specified cause and a detail message.
	 *
	 * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that the cause
	 *              is nonexistent or unknown.)
	 */
	public CompressException(final Throwable cause) {
		super(cause);
	}
}
