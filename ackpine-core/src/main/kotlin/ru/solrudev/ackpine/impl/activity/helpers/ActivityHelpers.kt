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

package ru.solrudev.ackpine.impl.activity.helpers

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.view.WindowManager

@Suppress("DEPRECATION")
@JvmSynthetic
internal fun Activity.turnScreenOnWhenLocked() {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
		setShowWhenLocked(true)
		setTurnScreenOn(true)
	} else {
		window.addFlags(
			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
					or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
					or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
		)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
		}
	}
	with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			requestDismissKeyguard(this@turnScreenOnWhenLocked, null)
		}
	}
}

@Suppress("DEPRECATION")
@JvmSynthetic
internal fun Activity.clearTurnScreenOnSettings() {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
		setShowWhenLocked(false)
		setTurnScreenOn(false)
	} else {
		window.clearFlags(
			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
					or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
					or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
		)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
		}
	}
}