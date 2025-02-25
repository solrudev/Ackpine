/*
 * Copyright (C) 2024 Ilya Fomichev
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

/**
 * Represents a result of evaluation of [APK split][Apk] compatibility with a device.
 */
@Deprecated(
	message = "This class is used for deprecated APK sequence transformation APIs as an intermediate value holder. " +
			"Migrate to SplitPackage which supports use cases for which this class was intended. Usage of this class " +
			"will become an error in the next minor release.",
	level = DeprecationLevel.ERROR,
	replaceWith = ReplaceWith(
		expression = "SplitPackage.Entry",
		imports = ["ru.solrudev.ackpine.splits.SplitPackage"]
	)
)
public data class ApkCompatibility(

	/**
	 * Indicates whether the [apk] is the most preferred for the device among other splits of the same type.
	 */
	public val isPreferred: Boolean,

	/**
	 * An [APK split][Apk].
	 */
	public val apk: Apk
)