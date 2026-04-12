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

package ru.solrudev.ackpine.libsu

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.ServiceManager
import android.os.UserHandleHidden
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import org.lsposed.hiddenapibypass.HiddenApiBypass
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.impl.services.PackageInstallerSessionWrapper
import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class RootPackageInstaller(
	private val rootService: IBinder,
	private val packageInstaller: PackageInstaller,
	private val remotePackageInstaller: IPackageInstaller
) : PackageInstallerService {

	override val uid = 0
	private val installParameters = ConcurrentHashMap<UUID, LibsuPlugin.InstallParameters>()
	private val uninstallParameters = ConcurrentHashMap<UUID, LibsuPlugin.UninstallParameters>()

	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
		if (parameters is LibsuPlugin.InstallParameters) {
			installParameters[sessionId] = parameters
		}
		if (parameters is LibsuPlugin.UninstallParameters) {
			uninstallParameters[sessionId] = parameters
		}
	}

	override fun createSession(params: PackageInstaller.SessionParams, ackpineSessionId: UUID): Int {
		val libsuParams = installParameters[ackpineSessionId]
		if (libsuParams != null) {
			@Suppress("CAST_NEVER_SUCCEEDS")
			applyInstallFlags(params as PackageInstallerHidden.SessionParams, libsuParams)
			if (libsuParams.installerPackageName.isNotEmpty() && Build.VERSION.SDK_INT >= 28) {
				@Suppress("NewApi") // method is available since API 28, but was hidden before API 34
				params.setInstallerPackageName(libsuParams.installerPackageName)
			}
		}
		return packageInstaller.createSession(params)
	}

	override fun openSession(sessionId: Int): PackageInstallerService.Session {
		val remoteSession = IPackageInstallerSession.Stub.asInterface(
			RootProxyBinderWrapper(rootService, remotePackageInstaller.openSession(sessionId).asBinder())
		)

		@Suppress("CAST_NEVER_SUCCEEDS")
		val session = PackageInstallerHidden.Session(remoteSession) as PackageInstaller.Session
		return PackageInstallerSessionWrapper(session)
	}

	override fun getSessionInfo(sessionId: Int) = packageInstaller.getSessionInfo(sessionId)

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	override fun commitSessionAfterInstallConstraintsAreMet(
		sessionId: Int,
		statusReceiver: IntentSender,
		constraints: PackageInstaller.InstallConstraints,
		timeoutMillis: Long
	) = packageInstaller.commitSessionAfterInstallConstraintsAreMet(
		sessionId,
		statusReceiver,
		constraints,
		timeoutMillis
	)

	override fun registerSessionCallback(callback: PackageInstaller.SessionCallback, handler: Handler) =
		packageInstaller.registerSessionCallback(callback, handler)

	override fun unregisterSessionCallback(callback: PackageInstaller.SessionCallback) =
		packageInstaller.unregisterSessionCallback(callback)

	override fun abandonSession(sessionId: Int) {
		remotePackageInstaller.abandonSession(sessionId)
	}

	@RequiresPermission(anyOf = [Manifest.permission.REQUEST_DELETE_PACKAGES, Manifest.permission.DELETE_PACKAGES])
	override fun uninstall(packageName: String, statusReceiver: IntentSender, ackpineSessionId: UUID) {
		if (Build.VERSION.SDK_INT < 27) {
			packageInstaller.uninstall(packageName, statusReceiver)
			return
		}
		val libsuParams = uninstallParameters[ackpineSessionId]
		var flags = 0
		if (libsuParams != null) {
			flags = applyFlag(flags, libsuParams.keepData, DELETE_KEEP_DATA)
			flags = applyFlag(flags, libsuParams.allUsers, DELETE_ALL_USERS)
		}
		@Suppress("CAST_NEVER_SUCCEEDS")
		(packageInstaller as PackageInstallerHidden).uninstall(packageName, flags, statusReceiver)
	}

	private fun applyInstallFlags(
		params: PackageInstallerHidden.SessionParams,
		libsuParams: LibsuPlugin.InstallParameters
	) {
		var flags = params.installFlags
		libsuParams.run {
			flags = applyFlag(flags, bypassLowTargetSdkBlock, INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK)
			flags = applyFlag(flags, allowTest, INSTALL_ALLOW_TEST)
			flags = applyFlag(flags, replaceExisting, INSTALL_REPLACE_EXISTING)
			flags = applyFlag(flags, requestDowngrade, INSTALL_REQUEST_DOWNGRADE or INSTALL_ALLOW_DOWNGRADE)
			flags = applyFlag(flags, grantAllRequestedPermissions, INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS)
			flags = applyFlag(flags, allUsers, INSTALL_ALL_USERS)
		}
		params.installFlags = flags
	}

	private fun applyFlag(flags: Int, isFlagPresent: Boolean, flag: Int): Int {
		if (isFlagPresent) {
			return flags or flag
		}
		return flags and flag.inv()
	}

	internal companion object Factory {

		@JvmSynthetic
		internal fun create(context: Context): RootPackageInstaller {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				HiddenApiBypass.addHiddenApiExemptions(
					"Landroid/content/pm/IPackageManager",
					"Landroid/content/pm/IPackageInstaller",
					"Landroid/content/pm/IPackageInstallerSession",
					"Landroid/content/pm/PackageInstaller",
					"Landroid/os/UserHandle",
					"Landroid/os/ServiceManager"
				)
			}
			val rootService = RootProxyService.bind(context)
			val remotePackageManager = IPackageManager.Stub.asInterface(
				RootProxyBinderWrapper(rootService, checkNotNull(ServiceManager.getService("package")))
			)
			val remotePackageInstaller = IPackageInstaller.Stub.asInterface(
				RootProxyBinderWrapper(rootService, remotePackageManager.packageInstaller.asBinder())
			)
			return RootPackageInstaller(
				rootService,
				createPackageInstaller(
					context,
					remotePackageInstaller,
					context.packageName,
					UserHandleHidden.myUserId()
				),
				remotePackageInstaller
			)
		}

		@Suppress("CAST_NEVER_SUCCEEDS")
		private fun createPackageInstaller(
			context: Context,
			remotePackageInstaller: IPackageInstaller,
			installerPackageName: String,
			userId: Int
		) = when {
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PackageInstallerHidden(
				remotePackageInstaller,
				installerPackageName,
				context.attributionTag,
				userId
			)

			Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> PackageInstallerHidden(
				remotePackageInstaller,
				installerPackageName,
				userId
			)

			else -> context.applicationContext.let { applicationContext ->
				PackageInstallerHidden(
					applicationContext,
					applicationContext.packageManager,
					remotePackageInstaller,
					installerPackageName,
					userId
				)
			}
		} as PackageInstaller
	}
}