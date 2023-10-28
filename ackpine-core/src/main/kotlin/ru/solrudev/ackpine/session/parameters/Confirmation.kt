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

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.VIBRATE
import ru.solrudev.ackpine.session.parameters.Confirmation.DEFERRED
import ru.solrudev.ackpine.session.parameters.Confirmation.IMMEDIATE

/**
 * A strategy for handling user's confirmation of installation or uninstallation.
 *
 * * [IMMEDIATE] &mdash; user will be prompted to confirm installation or uninstallation right away. Suitable for
 * 	 launching session directly from the UI when app is in foreground.
 * * [DEFERRED] (default) &mdash; user will be shown a high-priority notification which will launch confirmation
 *   activity.
 */
public enum class Confirmation {

	/**
	 * Prompt user to confirm installation or uninstallation right away.
	 */
	IMMEDIATE,

	/**
	 * Show a high-priority notification which will launch confirmation activity to a user.
	 *
	 * Requires [VIBRATE], [POST_NOTIFICATIONS] permissions.
	 */
	DEFERRED
}