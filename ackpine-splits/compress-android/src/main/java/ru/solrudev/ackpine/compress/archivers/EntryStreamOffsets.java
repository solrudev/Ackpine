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
package ru.solrudev.ackpine.compress.archivers;

/**
 * Provides information about ArchiveEntry stream offsets.
 */
public interface EntryStreamOffsets {

	/**
	 * Indicates that the offset is unknown.
	 */
	long OFFSET_UNKNOWN = -1;

	/**
	 * Gets the offset of data stream within the archive file,
	 *
	 * @return the offset of entry data stream, {@code OFFSET_UNKNOWN} if not known.
	 */
	long getDataOffset();

	/**
	 * Tests whether the stream is contiguous, that is, not split among several archive parts, interspersed with control blocks, and so on.
	 *
	 * @return true if stream is contiguous, false otherwise.
	 */
	boolean isStreamContiguous();
}
