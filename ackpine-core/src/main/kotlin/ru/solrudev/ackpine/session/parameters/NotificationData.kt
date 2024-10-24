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

package ru.solrudev.ackpine.session.parameters

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo

/**
 * Data for a high-priority notification which launches confirmation activity.
 */
public class NotificationData private constructor(

	/**
	 * Notification icon.
	 *
	 * Default value is [android.R.drawable.ic_dialog_alert].
	 */
	@DrawableRes public val icon: Int,

	/**
	 * Notification title.
	 */
	public val title: ResolvableString,

	/**
	 * Notification text.
	 */
	public val contentText: ResolvableString
) {

	override fun toString(): String {
		return "NotificationData(icon=$icon, title=$title, contentText=$contentText)"
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as NotificationData
		if (title != other.title) return false
		if (contentText != other.contentText) return false
		if (icon != other.icon) return false
		return true
	}

	override fun hashCode(): Int {
		var result = title.hashCode()
		result = 31 * result + contentText.hashCode()
		result = 31 * result + icon
		return result
	}

	public companion object {

		/**
		 * Default notification data.
		 *
		 * Icon is [android.R.drawable.ic_dialog_alert], and default title and text are used when notification is
		 * displayed.
		 */
		@JvmField
		public val DEFAULT: NotificationData = NotificationData(
			icon = android.R.drawable.ic_dialog_alert,
			title = DefaultNotificationString,
			contentText = DefaultNotificationString
		)
	}

	/**
	 * Builder for [NotificationData].
	 */
	public class Builder {

		/**
		 * Notification icon.
		 *
		 * Default value is [android.R.drawable.ic_dialog_alert].
		 */
		@DrawableRes
		public var icon: Int = DEFAULT.icon
			private set

		/**
		 * Notification title.
		 *
		 * By default, a string from Ackpine library is used.
		 */
		public var title: ResolvableString = DEFAULT.title
			private set

		/**
		 * Notification text.
		 *
		 * By default, a string from Ackpine library is used.
		 */
		public var contentText: ResolvableString = DEFAULT.contentText
			private set

		/**
		 * Sets [NotificationData.icon].
		 */
		public fun setIcon(@DrawableRes icon: Int): Builder = apply {
			this.icon = icon
		}

		/**
		 * Sets [NotificationData.title].
		 */
		public fun setTitle(title: ResolvableString): Builder = apply {
			this.title = title
		}

		/**
		 * Sets [NotificationData.contentText].
		 */
		public fun setContentText(contentText: ResolvableString): Builder = apply {
			this.contentText = contentText
		}

		/**
		 * Constructs a new instance of [NotificationData].
		 */
		public fun build(): NotificationData = NotificationData(icon, title, contentText)
	}
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal data object DefaultNotificationString : ResolvableString {
	private const val serialVersionUID = 809543744617543082L
	override fun resolve(context: Context): String = ""
}