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

package ru.solrudev.ackpine.test

import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import java.util.UUID

/**
 * Factory for creating uninstall test sessions.
 *
 * This factory is invoked by [TestPackageUninstaller] when a session is created, configure it depending on provided
 * session parameters or with different [scripts][TestSessionScript]
 */
public fun interface TestUninstallSessionFactory {

	/**
	 * Creates a [TestUninstallSession] for the provided [sessionId] and [parameters].
	 */
	public fun create(
		sessionId: UUID,
		parameters: UninstallParameters
	): TestUninstallSession

	public companion object {
		@JvmSynthetic
		internal fun withScript(script: TestSessionScript<UninstallFailure>): TestUninstallSessionFactory {
			return TestUninstallSessionFactory { id, _ ->
				TestUninstallSession(script, id)
			}
		}
	}
}