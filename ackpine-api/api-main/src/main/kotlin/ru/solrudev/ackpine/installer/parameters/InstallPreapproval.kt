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

import android.content.pm.PackageInstaller
import android.icu.util.ULocale
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * Details for requesting the pre-commit install approval.
 *
 * Takes effect only on API level >= [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED]
 * installer type.
 *
 * @see [PackageInstaller.Session.requestUserPreapproval]
 */
public class InstallPreapproval private constructor(

	/**
	 * The package name of the app to be installed.
	 */
	public val packageName: String,

	/**
	 * The label representing the app to be installed.
	 */
	public val label: String,

	/**
	 * The locale of the app label being installed. Represented by IETF BCP 47 language tag.
	 */
	public val languageTag: String,

	/**
	 * The icon representing the app to be installed.
	 */
	public val icon: Uri
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as InstallPreapproval
		if (packageName != other.packageName) return false
		if (label != other.label) return false
		if (languageTag != other.languageTag) return false
		if (icon != other.icon) return false
		return true
	}

	override fun hashCode(): Int {
		var result = packageName.hashCode()
		result = 31 * result + label.hashCode()
		result = 31 * result + languageTag.hashCode()
		result = 31 * result + icon.hashCode()
		return result
	}

	override fun toString(): String {
		return "InstallPreapproval(" +
				"packageName='$packageName', " +
				"label='$label', " +
				"languageTag='$languageTag', " +
				"icon=$icon" +
				")"
	}

	/**
	 * Builder for [InstallPreapproval].
	 */
	public class Builder(
		private val packageName: String,
		private val label: String,
		private val languageTag: String
	) {

		@RequiresApi(Build.VERSION_CODES.N)
		public constructor(
			packageName: String,
			label: String,
			locale: ULocale
		) : this(packageName, label, locale.toLanguageTag())

		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		public constructor(
			packageName: String,
			label: String,
			locale: Locale
		) : this(packageName, label, locale.toLanguageTag())

		/**
		 * The icon representing the app to be installed.
		 */
		public var icon: Uri = Uri.EMPTY
			private set

		/**
		 * Sets [InstallPreapproval.icon].
		 */
		public fun setIcon(icon: Uri): Builder = apply {
			this.icon = icon
		}

		/**
		 * Constructs a new instance of [InstallPreapproval].
		 */
		public fun build(): InstallPreapproval {
			return InstallPreapproval(packageName, label, languageTag, icon)
		}
	}

	public companion object {

		/**
		 * Default [InstallPreapproval], which is no preapproval.
		 */
		@JvmField
		public val NONE: InstallPreapproval = InstallPreapproval(
			packageName = "",
			label = "",
			languageTag = "",
			icon = Uri.EMPTY
		)
	}
}