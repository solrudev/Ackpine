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
import ru.solrudev.ackpine.remote.RemoteSession
import ru.solrudev.ackpine.session.parameters.Confirmation

/**
 * DSL allowing to configure user-facing confirmation for the [RemoteSession].
 */
public interface RemoteConfirmationDsl {

	/**
	 * A strategy for handling user's confirmation of installation or uninstallation.
	 *
	 * Default strategy is [Confirmation.DEFERRED].
	 */
	public var confirmation: Confirmation

	/**
	 * Data for a high-priority notification which launches confirmation activity.
	 *
	 * Default value is [RemoteNotificationData.DEFAULT].
	 *
	 * Ignored when [confirmation] is [Confirmation.IMMEDIATE].
	 */
	public var notificationData: RemoteNotificationData
}

/**
 * Configures [notification DSL][RemoteNotificationDataDsl].
 */
public inline fun RemoteConfirmationDsl.notification(configure: RemoteNotificationDataDsl.() -> Unit) {
	this.notificationData = RemoteNotificationDataDslBuilder().apply(configure).build()
}