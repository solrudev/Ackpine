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

package ru.solrudev.ackpine.installer.parameters

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class InstallConstraintsTest {

	@Test
	fun negativeTimeoutThrows() {
		assertFailsWith<IllegalArgumentException> {
			InstallConstraints.Builder(-1L)
		}
		assertFailsWith<IllegalArgumentException> {
			InstallConstraints.Builder(Duration.ofMillis(-1))
		}
		assertFailsWith<IllegalArgumentException> {
			InstallConstraints.gentleUpdate(-1L)
		}
		assertFailsWith<IllegalArgumentException> {
			InstallConstraints.gentleUpdate(Duration.ofMillis(-1))
		}
		assertFailsWith<IllegalArgumentException> {
			InstallConstraints.gentleUpdate((-1).milliseconds)
		}
	}
}