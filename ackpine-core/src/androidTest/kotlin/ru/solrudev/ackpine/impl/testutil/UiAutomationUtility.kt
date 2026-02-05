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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class UiAutomationUtility(
	private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
) {

	private val context: Context
		get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

	suspend fun pressBack() {
		device.ensureAwake()
		waitForIdle()
		device.pressBack()
	}

	suspend fun dismissDialog() {
		device.ensureAwake()
		waitForIdle()
		device.click((device.displayWidth * 0.5).toInt(), (device.displayHeight * 0.2).toInt())
	}

	suspend fun clickInstallOrUpdate(timeout: Duration = 30.seconds) {
		device.ensureAwake()
		waitForIdle()
		val button = device.wait(
			Until.findObject(By.text("Install|INSTALL|Update|UPDATE".toPattern())),
			timeout.inWholeMilliseconds
		) ?: assertionError("Install/Update button not found within $timeout")
		button.click()
		device.waitForIdle()
	}

	suspend fun clickCancel(timeout: Duration = 30.seconds) {
		device.ensureAwake()
		waitForIdle()
		val button = device.wait(
			Until.findObject(By.text("Cancel|CANCEL".toPattern())),
			timeout.inWholeMilliseconds
		) ?: assertionError("Cancel button not found within $timeout")
		if (context.isTv()) {
			device.pressDPadUp()
			device.pressDPadDown()
			device.pressEnter()
			return
		}
		button.click()
		device.waitForIdle()
	}

	suspend fun clickOk(timeout: Duration = 30.seconds) {
		if (!clickOkIfPresent(timeout)) {
			assertionError("OK button not found within $timeout")
		}
	}

	suspend fun clickOkIfPresent(timeout: Duration): Boolean {
		device.ensureAwake()
		waitForIdle()
		val button = device.wait(Until.findObject(By.text("Ok|OK".toPattern())), timeout.inWholeMilliseconds)
			?: return false
		if (context.isTv()) {
			device.pressDPadUp()
			device.pressEnter()
			return true
		}
		button.click()
		device.waitForIdle()
		return true
	}

	fun clickNotification(title: String, timeout: Duration = 30.seconds): Unit = device.run {
		ensureAwake()
		openNotification()
		wait(Until.hasObject(By.pkg("com.android.systemui")), timeout.inWholeMilliseconds)
		val notification = wait(Until.findObject(By.text(title)), timeout.inWholeMilliseconds)
			?: assertionError("Notification with title \"$title\" was not found.")
		notification.click()
	}

	suspend fun waitForIdle(delay: Duration = 300.milliseconds) {
		device.waitForIdle()
		val extraDelay = if (needsExtraStabilizationDelay()) 1.seconds else Duration.ZERO
		realtimeDelay(delay + extraDelay)
		device.waitForIdle()
	}

	fun revokeInstallPermission(packageName: String) {
		device.executeShellCommand("appops set $packageName REQUEST_INSTALL_PACKAGES ignore")
	}

	private suspend fun launchInstallerApp(installPermissionRequest: InstallPermissionRequest) = device.run {
		ensureAwake()
		val intent = context.packageManager
			.getLaunchIntentForPackage(ApkFixtures.INSTALLER_PACKAGE_NAME)
			?.putExtra("REQUEST_UNKNOWN_SOURCES", installPermissionRequest.requestUnknownSources)
			?: assertionError("${ApkFixtures.INSTALLER_PACKAGE_NAME} was not found.")
		context.startActivity(intent)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			InstrumentationRegistry
				.getInstrumentation()
				.uiAutomation
				.grantRuntimePermission(ApkFixtures.INSTALLER_PACKAGE_NAME, POST_NOTIFICATIONS)
		}
		val launchTimeout = if (needsExtraStabilizationDelay()) 3_000L else 1_000L
		wait(Until.hasObject(By.pkg(ApkFixtures.INSTALLER_PACKAGE_NAME)), launchTimeout)
		grantInstallPermission(installPermissionRequest, 1.seconds)
	}

	suspend fun launchInstallerApp(
		installPermissionRequest: InstallPermissionRequest,
		timeout: Duration = 30.seconds
	) {
		val packageWaitTimeout = if (needsExtraStabilizationDelay()) 3_000L else 1_000L
		val found = waitForCondition(timeout) {
			launchInstallerApp(installPermissionRequest)
			device.wait(Until.hasObject(By.pkg(ApkFixtures.INSTALLER_PACKAGE_NAME)), packageWaitTimeout)
		}
		if (!found) {
			assertionError("Package ${ApkFixtures.INSTALLER_PACKAGE_NAME} could not be launched after $timeout")
		}
	}

	suspend fun grantInstallPermission(
		installPermissionRequest: InstallPermissionRequest,
		timeout: Duration = 10.seconds
	) = device.run {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !installPermissionRequest.requestUnknownSources) {
			return
		}
		val canInstallPackages = canInstallPackages(installPermissionRequest.packageName)
		if (canInstallPackages) {
			return
		}
		if (installPermissionRequest is InstallPermissionRequest.Installing) {
			wait(Until.findObject(By.text("SETTINGS")), timeout.inWholeMilliseconds)?.click() ?: return
			waitForIdle()
		}
		val unknownSourcesSwitch = wait(
			Until.findObject(By.text(installPermissionRequest.permissionSwitchText)),
			timeout.inWholeMilliseconds
		) ?: return
		unknownSourcesSwitch.click()
		waitForIdle()
		waitForCondition(timeout = 3.seconds) {
			canInstallPackages(installPermissionRequest.packageName)
		}
		if (installPermissionRequest.returnAfterGranting) {
			waitForIdle()
			pressBack()
		}
	}

	suspend fun forceStopInstallerApp() = killInstaller("force-stop", wait = false)
	suspend fun hideAndKillInstallerApp() = killInstaller("kill", wait = true)

	fun openRecentInstallerApp(timeout: Duration = 1.seconds): Unit = device.run {
		ensureAwake()
		pressRecentApps()
		wait(Until.hasObject(By.res("recent_apps")), timeout.inWholeMilliseconds)
		click((displayWidth * 0.5).toInt(), (displayHeight * 0.5).toInt())
	}

	private suspend fun killInstaller(
		command: String,
		wait: Boolean,
		timeout: Duration = 30.seconds
	) = device.run {
		ensureAwake()
		pressHome()
		waitForIdle()
		val killed = waitForCondition(timeout) {
			executeShellCommand("am $command ${ApkFixtures.INSTALLER_PACKAGE_NAME}")
			if (Build.VERSION.SDK_INT in 24..27) {
				val pid = executeShellCommand("pidof ${ApkFixtures.INSTALLER_PACKAGE_NAME}").trim()
				if (pid.isNotEmpty()) {
					executeShellCommand("su 0 kill -9 $pid")
				}
			}
			val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				executeShellCommand("pidof ${ApkFixtures.INSTALLER_PACKAGE_NAME}")
			} else {
				executeShellCommand("ps -A | grep ${ApkFixtures.INSTALLER_PACKAGE_NAME}")
			}
			result.trim().isEmpty()
		}
		if (!killed) {
			assertionError("${ApkFixtures.INSTALLER_PACKAGE_NAME} process could not be killed after $timeout")
		}
		waitForIdle()
		if (wait || needsExtraStabilizationDelay()) {
			realtimeDelay(2.seconds)
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
			.trim()
			.substringBefore(';')
		return isGranted == "REQUEST_INSTALL_PACKAGES: allow"
	}

	private suspend inline fun waitForCondition(
		timeout: Duration = 30.seconds,
		initialDelay: Duration = 100.milliseconds,
		maxDelay: Duration = 2.seconds,
		backoffMultiplier: Double = 1.5,
		condition: () -> Boolean
	): Boolean {
		val endTime = System.currentTimeMillis() + timeout.inWholeMilliseconds
		var currentDelay = initialDelay
		while (System.currentTimeMillis() < endTime) {
			if (condition()) {
				return true
			}
			device.waitForIdle()
			realtimeDelay(currentDelay)
			currentDelay = (currentDelay * backoffMultiplier).coerceAtMost(maxDelay)
		}
		return false
	}
}