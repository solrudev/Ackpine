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

package ru.solrudev.ackpine.impl.plugability

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AckpineServiceLazyTest {

	@Test
	fun isInitializedReflectsLazyState() {
		var factoryCalls = 0
		val service = RecordingTestService()
		val lazy = AckpineServiceLazy {
			factoryCalls++
			service
		}

		assertFalse(lazy.isInitialized())

		assertSame(service, lazy.value)
		assertTrue(lazy.isInitialized())
		assertEquals(1, factoryCalls)
	}

	@Test
	fun applyParametersDefersUntilServiceInitialization() {
		val service = RecordingTestService()
		var factoryCalls = 0
		val lazy = AckpineServiceLazy {
			factoryCalls++
			service
		}
		val firstSessionId = UUID.randomUUID()
		val secondSessionId = UUID.randomUUID()
		val firstParameters = TestParams("first")
		val secondParameters = TestParams("second")

		lazy.applyParameters(firstSessionId, firstParameters)
		lazy.applyParameters(secondSessionId, secondParameters)

		assertFalse(lazy.isInitialized())
		assertTrue(service.appliedParameters.isEmpty())

		lazy.value

		assertTrue(lazy.isInitialized())
		assertEquals(1, factoryCalls)
		val expectedParameters = listOf(
			AppliedParameters(firstSessionId, firstParameters),
			AppliedParameters(secondSessionId, secondParameters)
		)
		assertEquals(expectedParameters, service.appliedParameters)
	}

	@Test
	fun applyParametersDelegatesImmediatelyAfterInitialization() {
		val service = RecordingTestService()
		val lazy = AckpineServiceLazy { service }
		val sessionId = UUID.randomUUID()
		val parameters = TestParams("value")

		assertSame(service, lazy.value)

		lazy.applyParameters(sessionId, parameters)

		assertEquals(listOf(AppliedParameters(sessionId, parameters)), service.appliedParameters)
	}
}