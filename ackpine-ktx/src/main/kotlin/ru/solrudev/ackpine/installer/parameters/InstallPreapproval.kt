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

package ru.solrudev.ackpine.installer.parameters

import android.icu.util.ULocale
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * Constructs a new instance of [InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param languageTag the locale of the app label being installed. Represented by IETF BCP 47 language tag.
 */
public inline fun InstallPreapproval(
	packageName: String,
	label: String,
	languageTag: String,
	configure: InstallPreapprovalDsl.() -> Unit = {}
): InstallPreapproval {
	return InstallPreapprovalDslBuilder(packageName, label, languageTag).apply(configure).build()
}

/**
 * Constructs a new instance of [InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being installed.
 */
@RequiresApi(Build.VERSION_CODES.N)
public inline fun InstallPreapproval(
	packageName: String,
	label: String,
	locale: ULocale,
	configure: InstallPreapprovalDsl.() -> Unit = {}
): InstallPreapproval {
	return InstallPreapprovalDslBuilder(packageName, label, locale).apply(configure).build()
}

/**
 * Constructs a new instance of [InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being installed.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline fun InstallPreapproval(
	packageName: String,
	label: String,
	locale: Locale,
	configure: InstallPreapprovalDsl.() -> Unit = {}
): InstallPreapproval {
	return InstallPreapprovalDslBuilder(packageName, label, locale).apply(configure).build()
}

/**
 * Constructs a new instance of [InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param languageTag the locale of the app label being installed. Represented by IETF BCP 47 language tag.
 * @param icon the icon representing the app to be installed.
 */
public fun InstallPreapproval(
	packageName: String,
	label: String,
	languageTag: String,
	icon: Uri
): InstallPreapproval {
	return InstallPreapproval.Builder(packageName, label, languageTag).setIcon(icon).build()
}

/**
 * Constructs a new instance of [InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being installed.
 * @param icon the icon representing the app to be installed.
 */
@RequiresApi(Build.VERSION_CODES.N)
public fun InstallPreapproval(
	packageName: String,
	label: String,
	locale: ULocale,
	icon: Uri
): InstallPreapproval {
	return InstallPreapproval.Builder(packageName, label, locale).setIcon(icon).build()
}

/**
 * Constructs a new instance of [InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being installed.
 * @param icon the icon representing the app to be installed.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public fun InstallPreapproval(
	packageName: String,
	label: String,
	locale: Locale,
	icon: Uri
): InstallPreapproval {
	return InstallPreapproval.Builder(packageName, label, locale).setIcon(icon).build()
}