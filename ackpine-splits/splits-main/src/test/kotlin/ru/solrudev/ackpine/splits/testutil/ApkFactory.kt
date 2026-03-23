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

package ru.solrudev.ackpine.splits.testutil

import android.net.Uri
import ru.solrudev.ackpine.splits.Abi
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.Dpi
import java.util.Locale

private const val DEFAULT_PACKAGE = "com.example.app"
private const val DEFAULT_VERSION_CODE = 1L

fun createBaseApk(
	name: String = "base",
	packageName: String = DEFAULT_PACKAGE,
	versionCode: Long = DEFAULT_VERSION_CODE,
	versionName: String = "1.0.0"
) = Apk.Base(
	uri = Uri.EMPTY,
	name = name,
	size = 0L,
	packageName = packageName,
	versionCode = versionCode,
	versionName = versionName
)

fun createLibsApk(
	name: String = "config.arm64_v8a",
	packageName: String = DEFAULT_PACKAGE,
	versionCode: Long = DEFAULT_VERSION_CODE,
	abi: Abi = Abi.ARM64_V8A,
	configForSplit: String = ""
) = Apk.Libs(
	uri = Uri.EMPTY,
	name = name,
	size = 0L,
	packageName = packageName,
	versionCode = versionCode,
	abi = abi,
	configForSplit = configForSplit
)

fun createScreenDensityApk(
	name: String = "config.xxhdpi",
	packageName: String = DEFAULT_PACKAGE,
	versionCode: Long = DEFAULT_VERSION_CODE,
	dpi: Dpi = Dpi.XXHDPI,
	configForSplit: String = ""
) = Apk.ScreenDensity(
	uri = Uri.EMPTY,
	name = name,
	size = 0L,
	packageName = packageName,
	versionCode = versionCode,
	dpi = dpi,
	configForSplit = configForSplit
)

fun createLocalizationApk(
	name: String = "config.en",
	packageName: String = DEFAULT_PACKAGE,
	versionCode: Long = DEFAULT_VERSION_CODE,
	locale: Locale = Locale("en"),
	configForSplit: String = ""
) = Apk.Localization(
	uri = Uri.EMPTY,
	name = name,
	size = 0L,
	packageName = packageName,
	versionCode = versionCode,
	locale = locale,
	configForSplit = configForSplit
)

fun createFeatureApk(
	name: String = "dynamic_feature",
	packageName: String = DEFAULT_PACKAGE,
	versionCode: Long = DEFAULT_VERSION_CODE
) = Apk.Feature(
	uri = Uri.EMPTY,
	name = name,
	size = 0L,
	packageName = packageName,
	versionCode = versionCode
)

fun createOtherApk(
	name: String = "config.other",
	packageName: String = DEFAULT_PACKAGE,
	versionCode: Long = DEFAULT_VERSION_CODE,
	configForSplit: String = ""
) = Apk.Other(
	uri = Uri.EMPTY,
	name = name,
	size = 0L,
	packageName = packageName,
	versionCode = versionCode,
	configForSplit = configForSplit
)