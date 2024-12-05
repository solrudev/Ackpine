/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

@file:Suppress("Unused", "ConstPropertyName")

package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.session.Failure
import java.io.Serializable

/**
 * Represents the cause of uninstallation failure.
 */
public sealed interface UninstallFailure : Failure, Serializable {

	/**
	 * The operation failed in a generic way.
	 */
	public data object Generic : UninstallFailure {
		private const val serialVersionUID = -6110974914043192127L
	}

	/**
	 * The operation failed because it was actively aborted.
	 * For example, the user actively declined uninstall request.
	 */
	public data class Aborted(public val message: String) : UninstallFailure {
		private companion object {
			private const val serialVersionUID = -2386460202828522962L
		}
	}

	/**
	 * The operation failed because an exception was thrown.
	 */
	public data class Exceptional(public override val exception: Exception) : UninstallFailure, Failure.Exceptional {
		private companion object {
			private const val serialVersionUID = -3918656046001035393L
		}
	}

	@Suppress("unused")
	private data object NonExhaustiveWhenGuard : UninstallFailure {
		private const val serialVersionUID = 6803470565073569530L
		private fun readResolve(): Any = NonExhaustiveWhenGuard
	}
}