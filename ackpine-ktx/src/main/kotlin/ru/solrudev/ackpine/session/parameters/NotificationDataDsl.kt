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

package ru.solrudev.ackpine.session.parameters

import ru.solrudev.ackpine.resources.ResolvableString

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
	public var icon: DrawableId

	/**
	 * Notification title.
	 *
	 * By default, a string from Ackpine library is used.
	 */
	public var title: ResolvableString

	/**
	 * Notification text.
	 *
	 * By default, a string from Ackpine library is used.
	 */
	public var contentText: ResolvableString
}

@PublishedApi
internal class NotificationDataDslBuilder : NotificationDataDsl {

	private val builder = NotificationData.Builder()

	override var icon: DrawableId
		get() = builder.icon
		set(value) {
			builder.setIcon(value)
		}

	override var title: ResolvableString
		get() = builder.title
		set(value) {
			builder.setTitle(value)
		}

	override var contentText: ResolvableString
		get() = builder.contentText
		set(value) {
			builder.setContentText(value)
		}

	fun build() = builder.build()
}