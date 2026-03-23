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

package ru.solrudev.ackpine.splits

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AbiTest {

	@Test
	fun fromSplitNameReturnsCorrectAbiForAllEntries() {
		for (abi in Abi.entries) {
			val splitName = "config.${abi.name.lowercase()}"
			assertEquals(abi, Abi.fromSplitName(splitName))
		}
	}

	@Test
	fun fromSplitNameReturnsNullForUnknownAbi() {
		assertNull(Abi.fromSplitName("config.unknown_abi"))
	}

	@Test
	fun fromSplitNameReturnsNullForNoConfigPrefix() {
		assertNull(Abi.fromSplitName("arm64_v8a"))
	}

	@Test
	fun fromSplitNameIsCaseInsensitive() {
		assertEquals(Abi.ARM64_V8A, Abi.fromSplitName("config.ArM64_V8a"))
	}
}