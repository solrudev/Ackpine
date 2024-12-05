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
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import java.util.Locale

/**
 * DSL allowing to configure [pre-commit install approval][InstallPreapproval].
 */
@SessionParametersDsl
public interface InstallPreapprovalDsl {

	/**
	 * The icon representing the app to be installed.
	 */
	public var icon: Uri
}

@PublishedApi
internal class InstallPreapprovalDslBuilder(
	packageName: String,
	label: String,
	languageTag: String
) : InstallPreapprovalDsl {

	private val builder = InstallPreapproval.Builder(packageName, label, languageTag)

	@RequiresApi(Build.VERSION_CODES.N)
	constructor(
		packageName: String,
		label: String,
		locale: ULocale
	) : this(packageName, label, locale.toLanguageTag())

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	constructor(
		packageName: String,
		label: String,
		locale: Locale
	) : this(packageName, label, locale.toLanguageTag())

	override var icon: Uri
		get() = builder.icon
		set(value) {
			builder.setIcon(value)
		}

	fun build() = builder.build()
}