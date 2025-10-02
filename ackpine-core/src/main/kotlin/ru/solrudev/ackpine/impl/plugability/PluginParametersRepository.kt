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

package ru.solrudev.ackpine.impl.plugability

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.util.UUID

/**
 * A repository for [Ackpine plugin parameters][AckpinePlugin.Parameters].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@WorkerThread
public interface PluginParametersRepository {

	/**
	 * Returns plugin parameters for a session with ID equal to [sessionId].
	 */
	public fun getForSession(sessionId: UUID): AckpinePlugin.Parameters

	/**
	 * Writes provided plugin parameters for a session with ID equal to [sessionId].
	 */
	public fun setForSession(sessionId: UUID, params: AckpinePlugin.Parameters)
}

/**
 * [PluginParametersRepository] for [AckpinePlugin.Parameters.None].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object EmptyPluginParametersRepository : PluginParametersRepository {
	override fun getForSession(sessionId: UUID): AckpinePlugin.Parameters = AckpinePlugin.Parameters.None
	override fun setForSession(sessionId: UUID, params: AckpinePlugin.Parameters) { /* no-op */ }
}