/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.helpers

import android.content.Intent
import java.util.UUID

internal object SessionIdIntents {

	private const val EXTRA_SESSION_ID_MOST_SIG_BITS = "ru.solrudev.ackpine.extra.SESSION_ID_MOST_SIG_BITS"
	private const val EXTRA_SESSION_ID_LEAST_SIG_BITS = "ru.solrudev.ackpine.extra.SESSION_ID_LEAST_SIG_BITS"

	@JvmSynthetic
	internal fun getSessionId(intent: Intent): UUID {
		val mostSigBits = intent.getLongExtra(EXTRA_SESSION_ID_MOST_SIG_BITS, 0)
		val leastSigBits = intent.getLongExtra(EXTRA_SESSION_ID_LEAST_SIG_BITS, 0)
		return UUID(mostSigBits, leastSigBits)
	}

	@JvmSynthetic
	internal fun putSessionId(intent: Intent, sessionId: UUID) {
		intent.putExtra(EXTRA_SESSION_ID_MOST_SIG_BITS, sessionId.mostSignificantBits)
		intent.putExtra(EXTRA_SESSION_ID_LEAST_SIG_BITS, sessionId.leastSignificantBits)
	}
}