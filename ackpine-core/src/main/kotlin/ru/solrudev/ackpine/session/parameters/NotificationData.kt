package ru.solrudev.ackpine.session.parameters

import androidx.annotation.DrawableRes

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
	 *
	 * Empty by default. If empty, default title is used when notification is displayed.
	 */
	public val title: NotificationString,

	/**
	 * Notification text.
	 *
	 * Empty by default. If empty, default text is used when notification is displayed.
	 */
	public val contentText: NotificationString
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
			title = NotificationString.default(),
			contentText = NotificationString.default()
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
		 * Empty by default. If empty, default title is used when notification is displayed.
		 */
		public var title: NotificationString = DEFAULT.title
			private set

		/**
		 * Notification text.
		 *
		 * Empty by default. If empty, default text is used when notification is displayed.
		 */
		public var contentText: NotificationString = DEFAULT.contentText
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
		public fun setTitle(title: NotificationString): Builder = apply {
			this.title = title
		}

		/**
		 * Sets [NotificationData.contentText].
		 */
		public fun setContentText(contentText: NotificationString): Builder = apply {
			this.contentText = contentText
		}

		/**
		 * Constructs a new instance of [NotificationData].
		 */
		public fun build(): NotificationData = NotificationData(icon, title, contentText)
	}
}