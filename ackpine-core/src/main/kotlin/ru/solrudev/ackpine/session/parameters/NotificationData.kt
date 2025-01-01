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

@file:JvmName("NotificationDataConstants")
@file:Suppress("ConstPropertyName", "Unused")

package ru.solrudev.ackpine.session.parameters

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.resources.ResolvableString
import java.io.Serializable

/**
 * Data for a high-priority notification which launches confirmation activity.
 */
public class NotificationData private constructor(

	/**
	 * Notification icon.
	 *
	 * Default value is [android.R.drawable.ic_dialog_alert].
	 */
	public val icon: DrawableId,

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
		if (icon != other.icon) return false
		if (title != other.title) return false
		if (contentText != other.contentText) return false
		return true
	}

	override fun hashCode(): Int {
		var result = icon.hashCode()
		result = 31 * result + title.hashCode()
		result = 31 * result + contentText.hashCode()
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
			icon = DefaultNotificationIcon,
			title = DEFAULT_NOTIFICATION_STRING,
			contentText = DEFAULT_NOTIFICATION_STRING
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
		public var icon: DrawableId = DEFAULT.icon
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
		public fun setIcon(icon: DrawableId): Builder = apply {
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

/**
 * [Drawable] represented by Android resource ID.
 *
 * Should be explicitly subclassed to ensure stable persistence, and `serialVersionUID` must be present. Example:
 * ```
 * object InstallIcon : DrawableId {
 *     private const val serialVersionUID = 3692803605642002954L
 *     override fun drawableId() = R.drawable.ic_install
 *     private fun readResolve(): Any = InstallIcon
 * }
 * ```
 */
public interface DrawableId : Serializable {

	/**
	 * Returns an Android drawable resource ID.
	 */
	@DrawableRes
	public fun drawableId(): Int

	private companion object {
		private const val serialVersionUID = 6564416758029834576L
	}
}

private data object DefaultNotificationIcon : DrawableId {
	private const val serialVersionUID = 6906923061913799903L
	override fun drawableId() = android.R.drawable.ic_dialog_alert
	private fun readResolve(): Any = DefaultNotificationIcon
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@JvmSynthetic
@JvmField
internal val DEFAULT_NOTIFICATION_STRING = ResolvableString.raw("ACKPINE_DEFAULT_NOTIFICATION_STRING")