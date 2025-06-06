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
import java.io.InputStream;
import java.nio.ByteOrder;

import ru.solrudev.ackpine.compress.compressors.lzw.LZWInputStream;

/**
 * Input stream that decompresses ZIP method 1 (unshrinking). A variation of the LZW algorithm, with some twists.
 *
 * @NotThreadSafe
 * @since 1.7
 */
final class UnshrinkingInputStream extends LZWInputStream {
	private static final int MAX_CODE_SIZE = 13;
	private static final int MAX_TABLE_SIZE = 1 << MAX_CODE_SIZE;
	private final boolean[] isUsed;

	/**
	 * IOException is not actually thrown!
	 *
	 * @param inputStream
	 */
	UnshrinkingInputStream(final InputStream inputStream) {
		super(inputStream, ByteOrder.LITTLE_ENDIAN);
		setClearCode(DEFAULT_CODE_SIZE);
		initializeTables(MAX_CODE_SIZE);
		isUsed = new boolean[getPrefixesLength()];
		for (int i = 0; i < 1 << 8; i++) {
			isUsed[i] = true;
		}
		setTableSize(getClearCode() + 1);
	}

	@Override
	protected int addEntry(final int previousCode, final byte character) throws IOException {
		int tableSize = getTableSize();
		while (tableSize < MAX_TABLE_SIZE && isUsed[tableSize]) {
			tableSize++;
		}
		setTableSize(tableSize);
		final int idx = addEntry(previousCode, character, MAX_TABLE_SIZE);
		if (idx >= 0) {
			isUsed[idx] = true;
		}
		return idx;
	}

	@Override
	protected int decompressNextSymbol() throws IOException {
		//
		// table entry table entry
		// _____________ _____
		// table entry / \ / \
		// ____________/ \ \
		// / / \ / \ \
		// +---+---+---+---+---+---+---+---+---+---+
		// | . | . | . | . | . | . | . | . | . | . |
		// +---+---+---+---+---+---+---+---+---+---+
		// |<--------->|<------------->|<----->|<->|
		// symbol symbol symbol symbol
		//
		final int code = readNextCode();
		if (code < 0) {
			return -1;
		}
		if (code != getClearCode()) {
			boolean addedUnfinishedEntry = false;
			int effectiveCode = code;
			if (!isUsed[code]) {
				effectiveCode = addRepeatOfPreviousCode();
				addedUnfinishedEntry = true;
			}
			return expandCodeToOutputStack(effectiveCode, addedUnfinishedEntry);
		}
		final int subCode = readNextCode();
		if (subCode < 0) {
			throw new IOException("Unexpected EOF;");
		}
		if (subCode == 1) {
			if (getCodeSize() >= MAX_CODE_SIZE) {
				throw new IOException("Attempt to increase code size beyond maximum");
			}
			incrementCodeSize();
		} else if (subCode == 2) {
			partialClear();
			setTableSize(getClearCode() + 1);
		} else {
			throw new IOException("Invalid clear code subcode " + subCode);
		}
		return 0;
	}

	private void partialClear() {
		final boolean[] isParent = new boolean[MAX_TABLE_SIZE];
		for (int i = 0; i < isUsed.length; i++) {
			if (isUsed[i] && getPrefix(i) != UNUSED_PREFIX) {
				isParent[getPrefix(i)] = true;
			}
		}
		for (int i = getClearCode() + 1; i < isParent.length; i++) {
			if (!isParent[i]) {
				isUsed[i] = false;
				setPrefix(i, UNUSED_PREFIX);
			}
		}
	}
}
