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

	private const val EXTRA_SESSION_ID = "ru.solrudev.ackpine.extra.ACKPINE_SESSION_ID"
	private const val EXTRA_SESSION_ID_MOST_SIG_BITS = "ru.solrudev.ackpine.extra.SESSION_ID_MOST_SIG_BITS"
	private const val EXTRA_SESSION_ID_LEAST_SIG_BITS = "ru.solrudev.ackpine.extra.SESSION_ID_LEAST_SIG_BITS"
	private const val EXTRA_LEGACY_SESSION_ID = "ACKPINE_SESSION_ID"

	@JvmSynthetic
	internal fun getSessionId(intent: Intent, tag: String): UUID {
		val mostSigBits = intent.getLongExtra(EXTRA_SESSION_ID_MOST_SIG_BITS, -1L)
		val leastSigBits = intent.getLongExtra(EXTRA_SESSION_ID_LEAST_SIG_BITS, -1L)
		if (mostSigBits != -1L && leastSigBits != -1L) {
			return UUID(mostSigBits, leastSigBits)
		}
		val sessionId = intent.getSerializableExtraCompat<UUID>(EXTRA_SESSION_ID)
			?: intent.getSerializableExtraCompat(EXTRA_LEGACY_SESSION_ID)
		return requireNotNull(sessionId) { "$tag: ackpineSessionId was null" }
	}

	@JvmSynthetic
	internal fun putSessionId(intent: Intent, sessionId: UUID) {
		intent.putExtra(EXTRA_SESSION_ID_MOST_SIG_BITS, sessionId.mostSignificantBits)
		intent.putExtra(EXTRA_SESSION_ID_LEAST_SIG_BITS, sessionId.leastSignificantBits)
	}
}