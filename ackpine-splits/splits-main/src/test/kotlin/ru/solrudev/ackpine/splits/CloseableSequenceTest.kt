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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloseableSequenceTest {

	@Test
	fun yieldsValues() {
		val sequence = closeableSequence {
			yield(1)
			yield(2)
			yield(3)
		}
		assertEquals(listOf(1, 2, 3), sequence.toList())
	}

	@Test
	fun yieldsAllFromSequence() {
		val sequence = closeableSequence {
			yieldAll(sequenceOf(1, 2, 3))
		}
		assertEquals(listOf(1, 2, 3), sequence.toList())
	}

	@Test
	fun canOnlyBeConsumedOnce() {
		val sequence = closeableSequence {
			yield(1)
		}
		sequence.toList()
		assertFailsWith<IllegalStateException> {
			sequence.toList()
		}
	}

	@Test
	fun closeClosesRegisteredResources() {
		var closed = false
		val sequence = closeableSequence {
			AutoCloseable { closed = true }.use()
			yield(1)
		}
		val iterator = sequence.iterator()
		iterator.next()
		sequence.close()
		assertTrue(closed)
	}

	@Test
	fun closeUpdatesIsClosedFlag() {
		val sequence = closeableSequence {
			yield(1)
		}
		assertFalse(sequence.isClosed)
		sequence.close()
		assertTrue(sequence.isClosed)
	}
}