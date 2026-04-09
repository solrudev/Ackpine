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

package ru.solrudev.ackpine.capabilities

/**
 * Describes the availability level of an installer or uninstaller capability on the platform.
 */
public enum class CapabilityStatus {

	/**
	 * The capability is fully supported by the observable platform/API contract for the queried configuration.
	 *
	 * **Note:** This status **does not** guarantee that an operation with this capability used will succeed on a
	 * concrete device.
	 */
	SUPPORTED,

	/**
	 * The capability will affect operations for the queried configuration, but its behavior may be unreliable
	 * or inconsistent across devices or Android versions.
	 */
	UNRELIABLE,

	/**
	 * The capability is not supported for the queried configuration.
	 */
	UNSUPPORTED;

	/**
	 * Returns `true` if the capability is fully supported by the observable platform/API contract for the queried
	 * configuration.
	 *
	 * **Note:** [SUPPORTED] status **does not** guarantee that an operation with this capability used will succeed on a
	 * concrete device.
	 */
	public val isSupported: Boolean
		get() = this == SUPPORTED

	/**
	 * Returns `true` if the capability will affect operations for the queried configuration,
	 * [reliably][SUPPORTED] or [not][UNRELIABLE].
	 */
	public val isAvailable: Boolean
		get() = this != UNSUPPORTED

	/**
	 * Returns `true` if the capability is not supported for the queried configuration.
	 */
	public val isUnavailable: Boolean
		get() = this == UNSUPPORTED
}