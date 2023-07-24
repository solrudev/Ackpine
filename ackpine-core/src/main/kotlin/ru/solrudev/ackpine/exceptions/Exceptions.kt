/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.exceptions

import android.os.Build

/**
 * Thrown if installation of split packages is not supported when creating session with split package is attempted.
 */
public class SplitPackagesNotSupportedException : IllegalArgumentException(
	"Split packages are not supported on current Android API level: ${Build.VERSION.SDK_INT}"
)

/**
 * Thrown if Ackpine initialization is attempted more than once.
 */
public class AckpineReinitializeException : Exception(
	"Attempt of Ackpine re-initialization. Make sure you're not initializing Ackpine manually without disabling " +
			"automatic initialization."
)