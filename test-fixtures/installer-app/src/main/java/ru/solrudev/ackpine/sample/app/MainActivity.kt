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

package ru.solrudev.ackpine.sample.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings

class MainActivity : Activity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val requestUnknownSources = intent.getBooleanExtra("REQUEST_UNKNOWN_SOURCES", true)
		if (!requestUnknownSources || Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls()) {
			return
		}
		startActivity(
			Intent(
				Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
				Uri.parse("package:$packageName")
			)
		)
	}
}