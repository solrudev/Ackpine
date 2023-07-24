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

import ru.solrudev.ackpine.session.Session

/**
 * DSL allowing to configure user-facing confirmation for the [Session].
 */
public interface ConfirmationDsl : ConfirmationAware {
	override var confirmation: Confirmation
	override var notificationData: NotificationData
}

/**
 * Configures [notification DSL][NotificationDataDsl].
 */
public inline fun ConfirmationDsl.notification(configure: NotificationDataDsl.() -> Unit) {
	this.notificationData = NotificationData(configure)
}