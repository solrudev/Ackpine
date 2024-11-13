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

package ru.solrudev.ackpine.gradle

public object Constants {
	public const val PACKAGE_NAME: String = "ru.solrudev.ackpine"
	public const val JDK_VERSION: Int = 17
	public const val MIN_SDK: Int = 16
	public const val COMPILE_SDK: Int = 34
	public const val BUILD_TOOLS_VERSION: String = "34.0.0"
}

public object SampleConstants {
	public const val PACKAGE_NAME: String = "ru.solrudev.ackpine.sample"
	public const val MIN_SDK: Int = 21
	public const val TARGET_SDK: Int = 34
}