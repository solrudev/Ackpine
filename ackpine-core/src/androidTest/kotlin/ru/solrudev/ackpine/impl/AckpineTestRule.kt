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

package ru.solrudev.ackpine.impl

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.UiAutomation
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class AckpineTestRule(
	private val allowUnknownSources: Boolean = true
) : TestRule {

	private val permissionRule = run {
		val permissions = buildSet {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				add(READ_EXTERNAL_STORAGE)
				add(WRITE_EXTERNAL_STORAGE)
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				add(POST_NOTIFICATIONS)
			}
		}
		GrantPermissionRule.grant(*permissions.toTypedArray())
	}

	override fun apply(base: Statement, description: Description): Statement {
		return AckpineTestStatement(
			permissionRule.apply(base, description),
			allowUnknownSources
		)
	}

	private class AckpineTestStatement(
		private val base: Statement,
		private val allowUnknownSources: Boolean
	) : Statement() {

		override fun evaluate() {
			val instrumentation = InstrumentationRegistry.getInstrumentation()
			val context = instrumentation.targetContext
			val uiAutomation = instrumentation.uiAutomation
			if (allowUnknownSources) {
				uiAutomation.allowAppOp(context.packageName, "REQUEST_INSTALL_PACKAGES")
				uiAutomation.allowAppOp(context.packageName, "REQUEST_DELETE_PACKAGES")
			}
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				uiAutomation.executeShellCommand("settings put secure install_non_market_apps 1").close()
			}
			base.evaluate()
		}

		private fun UiAutomation.allowAppOp(packageName: String, op: String) {
			executeShellCommand("appops set $packageName $op allow").close()
		}
	}
}