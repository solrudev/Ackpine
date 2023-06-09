package ru.solrudev.ackpine.session.parameters

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes

/**
 * DSL allowing to configure [high-priority notification for Session confirmation][NotificationData].
 */
@SessionParametersDsl
public interface NotificationDataDsl {

	/**
	 * Notification icon.
	 *
	 * Default value is [android.R.drawable.ic_dialog_alert].
	 */
	@set:SuppressLint("SupportAnnotationUsage")
	@get:DrawableRes
	@set:DrawableRes
	public var icon: Int

	/**
	 * Notification title.
	 *
	 * Empty by default. If empty, default title is used when notification is displayed.
	 */
	public var title: NotificationString

	/**
	 * Notification text.
	 *
	 * Empty by default. If empty, default text is used when notification is displayed.
	 */
	public var contentText: NotificationString
}

@PublishedApi
internal class NotificationDataDslBuilder : NotificationDataDsl {

	private val builder = NotificationData.Builder()

	override var icon: Int
		get() = builder.icon
		set(value) {
			builder.setIcon(value)
		}

	override var title: NotificationString
		get() = builder.title
		set(value) {
			builder.setTitle(value)
		}

	override var contentText: NotificationString
		get() = builder.contentText
		set(value) {
			builder.setContentText(value)
		}

	fun build() = builder.build()
}