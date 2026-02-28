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

package ru.solrudev.ackpine.remote.dsl

import ru.solrudev.ackpine.remote.RemoteNotificationData
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

/**
 * DSL allowing to configure [high-priority notification for Session confirmation][RemoteNotificationData].
 */
@SessionParametersDsl
public interface RemoteNotificationDataDsl {

	/**
	 * Notification title.
	 *
	 * By default, a string from Ackpine library is used.
	 */
	public var title: String

	/**
	 * Notification text.
	 *
	 * By default, a string from Ackpine library is used.
	 */
	public var contentText: String
}

@PublishedApi
internal class RemoteNotificationDataDslBuilder : RemoteNotificationDataDsl {
	override var title = ""
	override var contentText = ""
	fun build() = RemoteNotificationData(title, contentText)
}