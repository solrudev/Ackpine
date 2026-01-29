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

package ru.solrudev.ackpine.impl.testutil

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import ru.solrudev.ackpine.impl.ApkFixtures
import ru.solrudev.ackpine.impl.InstallPermissionRequest

class UiAutomationUtility(
	private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
) {

	private val context: Context
		get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

	fun pressBack() = device.run {
		ensureAwake()
		waitForIdle()
		pressBack()
	}

	fun dismissDialog() = device.run {
		ensureAwake()
		waitForIdle()
		click((displayWidth * 0.5).toInt(), (displayHeight * 0.2).toInt())
	}

	fun clickInstallOrUpdate(timeout: Long = 10_000): Unit = device.run {
		ensureAwake()
		wait(Until.findObject(By.text("Install|INSTALL|Update|UPDATE".toPattern())), timeout)?.click()
	}

	fun clickCancel(timeout: Long = 10_000): Unit = device.run {
		ensureAwake()
		wait(Until.findObject(By.text("Cancel|CANCEL".toPattern())), timeout)?.click()
		if (context.isTv()) {
			pressDPadUp()
			pressDPadDown()
			pressEnter()
		}
	}

	fun clickOk(timeout: Long = 10_000): Unit = device.run {
		ensureAwake()
		wait(Until.findObject(By.text("Ok|OK".toPattern())), timeout)?.click()
		if (context.isTv()) {
			pressDPadUp()
			pressEnter()
		}
	}

	fun clickNotification(title: String, timeout: Long = 10_000): Unit = device.run {
		ensureAwake()
		openNotification()
		wait(Until.hasObject(By.pkg("com.android.systemui")), timeout)
		val notification = wait(Until.findObject(By.text(title)), timeout)
			?: throw AssertionError("Notification with title \"$title\" was not found.")
		notification.click()
	}

	fun waitForIdle() = device.waitForIdle()

	fun revokeInstallPermission(packageName: String) {
		device.executeShellCommand("appops set $packageName REQUEST_INSTALL_PACKAGES ignore")
	}

	fun launchInstallerApp(
		installPermissionRequest: InstallPermissionRequest,
		timeout: Long = 1_000
	): Unit = device.run {
		ensureAwake()
		val intent = context.packageManager
			.getLaunchIntentForPackage(ApkFixtures.INSTALLER_PACKAGE_NAME)
			?.putExtra("REQUEST_UNKNOWN_SOURCES", installPermissionRequest.requestUnknownSources)
			?: error("${ApkFixtures.INSTALLER_PACKAGE_NAME} was not found.")
		context.startActivity(intent)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			InstrumentationRegistry
				.getInstrumentation()
				.uiAutomation
				.grantRuntimePermission(ApkFixtures.INSTALLER_PACKAGE_NAME, POST_NOTIFICATIONS)
		}
		wait(Until.hasObject(By.pkg(ApkFixtures.INSTALLER_PACKAGE_NAME)), timeout)
		grantInstallPermission(installPermissionRequest, timeout)
	}

	fun grantInstallPermission(
		installPermissionRequest: InstallPermissionRequest,
		timeout: Long = 10_000
	): Unit = device.run {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !installPermissionRequest.requestUnknownSources) {
			return
		}
		val canInstallPackages = canInstallPackages(installPermissionRequest.packageName)
		if (!canInstallPackages && installPermissionRequest is InstallPermissionRequest.Installing) {
			wait(Until.findObject(By.text("SETTINGS")), timeout)?.click() ?: return
			waitForIdle()
		}
		wait(
			Until.findObject(By.text(installPermissionRequest.permissionSwitchText)),
			timeout
		)?.let { unknownSourcesSwitch ->
			unknownSourcesSwitch.click()
			waitForIdle()
			Thread.sleep(1000)
			if (installPermissionRequest.returnAfterGranting) {
				pressBack()
			}
		}
	}

	fun forceStopInstallerApp() = killInstaller("force-stop", wait = false)
	fun hideAndKillInstallerApp() = killInstaller("kill", wait = true)

	fun openRecentInstallerApp(timeout: Long = 1_000): Unit = device.run {
		ensureAwake()
		pressRecentApps()
		wait(Until.hasObject(By.res("recent_apps")), timeout)
		click((displayWidth * 0.5).toInt(), (displayHeight * 0.5).toInt())
	}

	private fun killInstaller(command: String, wait: Boolean) = device.run {
		ensureAwake()
		pressHome()
		waitForIdle()
		do {
			executeShellCommand("am $command ${ApkFixtures.INSTALLER_PACKAGE_NAME}")
			val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				executeShellCommand("pidof ${ApkFixtures.INSTALLER_PACKAGE_NAME}")
			} else {
				executeShellCommand("ps -A | grep ${ApkFixtures.INSTALLER_PACKAGE_NAME}")
			}
		} while (result.isNotEmpty())
		waitForIdle()
		if (wait || isAndroid11()) {
			Thread.sleep(2000)
		}
	}

	private fun UiDevice.ensureAwake() {
		if (!isScreenOn) {
			wakeUp()
		}
		waitForIdle()
	}

	private fun UiDevice.canInstallPackages(packageName: String): Boolean {
		val isGranted = executeShellCommand("appops get $packageName REQUEST_INSTALL_PACKAGES")
		return isGranted == "REQUEST_INSTALL_PACKAGES: allow"
	}
}