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

package ru.solrudev.ackpine.privileged

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.UserHandleHidden
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.helpers.concurrent.Locks
import ru.solrudev.ackpine.helpers.concurrent.computeIfAbsentCompat
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.impl.services.PackageInstallerSessionWrapper
import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Base [PackageInstallerService] implementation for backends that proxy package installer binders.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class PackageInstallerProxy protected constructor(
	private val context: Context,
	private val remotePackageInstaller: IPackageInstaller,
	private val installerPackageName: String,
	final override val uid: Int
) : PackageInstallerService {

	private val installParameters = ConcurrentHashMap<UUID, PrivilegedInstallParameters>()
	private val uninstallParameters = ConcurrentHashMap<UUID, PrivilegedUninstallParameters>()
	private val packageInstallers = ConcurrentHashMap<Int, PackageInstaller>()
	private val packageInstallerLocks = Locks(16)

	final override fun bind(sessionId: UUID): PackageInstallerService {
		val targetUser = installParameters[sessionId]?.targetUser
			?: uninstallParameters[sessionId]?.targetUser
			?: TargetUser.CURRENT
		val resolvedUserId = if (targetUser == TargetUser.CURRENT) {
			UserHandleHidden.myUserId()
		} else {
			targetUser.userId
		}
		val packageInstaller = packageInstallers.computeIfAbsentCompat(resolvedUserId, packageInstallerLocks) {
			createPackageInstaller(context, remotePackageInstaller, installerPackageName, resolvedUserId)
		}
		return BoundPackageInstaller(packageInstaller!!)
	}

	final override fun createSession(
		params: PackageInstaller.SessionParams,
		ackpineSessionId: UUID
	): Int = bind(ackpineSessionId).createSession(params, ackpineSessionId)

	final override fun openSession(sessionId: Int): PackageInstallerService.Session = unboundServiceAccess()
	final override fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo = unboundServiceAccess()

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	final override fun commitSessionAfterInstallConstraintsAreMet(
		sessionId: Int,
		statusReceiver: IntentSender,
		constraints: PackageInstaller.InstallConstraints,
		timeoutMillis: Long
	): Unit = unboundServiceAccess()

	final override fun registerSessionCallback(
		callback: PackageInstaller.SessionCallback,
		handler: Handler
	): Unit = unboundServiceAccess()

	final override fun unregisterSessionCallback(
		callback: PackageInstaller.SessionCallback
	): Unit = unboundServiceAccess()

	final override fun abandonSession(sessionId: Int): Unit = unboundServiceAccess()

	@RequiresPermission(anyOf = [Manifest.permission.REQUEST_DELETE_PACKAGES, Manifest.permission.DELETE_PACKAGES])
	final override fun uninstall(packageName: String, statusReceiver: IntentSender, ackpineSessionId: UUID) {
		bind(ackpineSessionId).uninstall(packageName, statusReceiver, ackpineSessionId)
	}

	private fun unboundServiceAccess(): Nothing = error(
		"PackageInstallerService must be bound to an Ackpine session before use"
	)

	private inner class BoundPackageInstaller(
		private val packageInstaller: PackageInstaller
	) : PackageInstallerService {

		override val uid: Int
			get() = this@PackageInstallerProxy.uid

		override fun bind(sessionId: UUID): PackageInstallerService = this@PackageInstallerProxy.bind(sessionId)

		override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) =
			this@PackageInstallerProxy.applyParameters(sessionId, parameters)

		override fun createSession(params: PackageInstaller.SessionParams, ackpineSessionId: UUID): Int {
			val privilegedParameters = installParameters[ackpineSessionId]
			if (privilegedParameters != null) {
				@Suppress("CAST_NEVER_SUCCEEDS")
				applyInstallFlags(params as PackageInstallerHidden.SessionParams, privilegedParameters)
				if (privilegedParameters.installerPackageName.isNotEmpty() && Build.VERSION.SDK_INT >= 28) {
					@Suppress("NewApi") // method is available since API 28, but was hidden before API 34
					params.setInstallerPackageName(privilegedParameters.installerPackageName)
				}
			}
			return packageInstaller.createSession(params)
		}

		override fun openSession(sessionId: Int): PackageInstallerService.Session {
			val remoteSession = IPackageInstallerSession.Stub.asInterface(
				wrapBinder(remotePackageInstaller.openSession(sessionId).asBinder())
			)

			@Suppress("CAST_NEVER_SUCCEEDS")
			val session = PackageInstallerHidden.Session(remoteSession) as PackageInstaller.Session
			return PackageInstallerSessionWrapper(session)
		}

		override fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo? =
			packageInstaller.getSessionInfo(sessionId)

		@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
		override fun commitSessionAfterInstallConstraintsAreMet(
			sessionId: Int,
			statusReceiver: IntentSender,
			constraints: PackageInstaller.InstallConstraints,
			timeoutMillis: Long
		): Unit = packageInstaller.commitSessionAfterInstallConstraintsAreMet(
			sessionId,
			statusReceiver,
			constraints,
			timeoutMillis
		)

		override fun registerSessionCallback(
			callback: PackageInstaller.SessionCallback,
			handler: Handler
		): Unit = packageInstaller.registerSessionCallback(callback, handler)

		override fun unregisterSessionCallback(callback: PackageInstaller.SessionCallback): Unit =
			packageInstaller.unregisterSessionCallback(callback)

		override fun abandonSession(sessionId: Int): Unit = packageInstaller.abandonSession(sessionId)

		@RequiresPermission(anyOf = [Manifest.permission.REQUEST_DELETE_PACKAGES, Manifest.permission.DELETE_PACKAGES])
		override fun uninstall(packageName: String, statusReceiver: IntentSender, ackpineSessionId: UUID) {
			if (Build.VERSION.SDK_INT < 27) {
				packageInstaller.uninstall(packageName, statusReceiver)
				return
			}
			val privilegedParameters = uninstallParameters[ackpineSessionId]
			var flags = 0
			if (privilegedParameters != null) {
				flags = applyFlag(flags, privilegedParameters.keepData, DELETE_KEEP_DATA)
				flags = applyFlag(flags, privilegedParameters.allUsers, DELETE_ALL_USERS)
				flags = applyFlag(flags, privilegedParameters.systemApp, DELETE_SYSTEM_APP)
			}
			@Suppress("CAST_NEVER_SUCCEEDS")
			(packageInstaller as PackageInstallerHidden).uninstall(packageName, flags, statusReceiver)
		}
	}

	protected fun applyInstallParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
		if (parameters is PrivilegedInstallParameters) {
			installParameters[sessionId] = parameters
		}
	}

	protected fun applyUninstallParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
		if (parameters is PrivilegedUninstallParameters) {
			uninstallParameters[sessionId] = parameters
		}
	}

	protected abstract fun wrapBinder(original: IBinder): IBinder

	@Suppress("CAST_NEVER_SUCCEEDS")
	private fun createPackageInstaller(
		context: Context,
		remotePackageInstaller: IPackageInstaller,
		installerPackageName: String,
		userId: Int
	): PackageInstaller = when {
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

	private fun applyInstallFlags(
		params: PackageInstallerHidden.SessionParams,
		privilegedParameters: PrivilegedInstallParameters
	) {
		var flags = params.installFlags
		privilegedParameters.run {
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
}