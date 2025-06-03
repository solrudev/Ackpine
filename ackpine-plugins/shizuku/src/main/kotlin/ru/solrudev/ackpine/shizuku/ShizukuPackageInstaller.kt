/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.shizuku

import android.content.Context
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import ru.solrudev.ackpine.impl.installer.PackageInstallerService
import ru.solrudev.ackpine.impl.installer.PackageInstallerSessionWrapper
import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [PackageInstallerService] which delegates work to [PackageInstaller] obtained through Shizuku.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ShizukuPackageInstaller(
	private val packageInstaller: PackageInstaller,
	private val remotePackageInstaller: IPackageInstaller,
	override val uid: Int
) : PackageInstallerService {

	private val pluginParameters = ConcurrentHashMap<UUID, ShizukuPlugin.Parameters>()

	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
		if (parameters is ShizukuPlugin.Parameters) {
			pluginParameters.put(sessionId, parameters)
		}
	}

	override fun createSession(params: PackageInstaller.SessionParams, ackpineSessionId: UUID): Int {
		val shizukuParams = pluginParameters[ackpineSessionId]
		if (shizukuParams != null) {
			applyInstallFlags(params, shizukuParams)
		}
		return packageInstaller.createSession(params)
	}

	override fun openSession(sessionId: Int): PackageInstallerService.Session {
		val remoteSession = IPackageInstallerSession.Stub.asInterface(
			ShizukuBinderWrapper(remotePackageInstaller.openSession(sessionId).asBinder())
		)
		val session = SESSION_CONSTRUCTOR.newInstance(remoteSession)
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

	override fun registerSessionCallback(callback: PackageInstaller.SessionCallback) =
		packageInstaller.registerSessionCallback(callback)

	override fun unregisterSessionCallback(callback: PackageInstaller.SessionCallback) =
		packageInstaller.unregisterSessionCallback(callback)

	override fun abandonSession(sessionId: Int) {
		remotePackageInstaller.abandonSession(sessionId)
	}

	private fun applyInstallFlags(
		params: PackageInstaller.SessionParams,
		shizukuParams: ShizukuPlugin.Parameters
	) {
		var flags = params.getInstallFlags()
		shizukuParams.run {
			flags = applyFlag(flags, bypassLowTargetSdkBlock, INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK)
			flags = applyFlag(flags, allowTest, INSTALL_ALLOW_TEST)
			flags = applyFlag(flags, replaceExisting, INSTALL_REPLACE_EXISTING)
			flags = applyFlag(flags, requestDowngrade, INSTALL_REQUEST_DOWNGRADE or INSTALL_ALLOW_DOWNGRADE)
			flags = applyFlag(flags, grantAllRequestedPermissions, INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS)
			flags = applyFlag(flags, allUsers, INSTALL_ALL_USERS)
		}
		params.setInstallFlags(flags)
	}

	private fun applyFlag(installFlags: Int, flag: Boolean, installFlag: Int): Int {
		if (flag) {
			return installFlags or installFlag
		}
		return installFlags and installFlag.inv()
	}

	internal companion object Factory {

		private val SESSION_CONSTRUCTOR = PackageInstaller.Session::class.java
			.getConstructor(IPackageInstallerSession::class.java)

		@JvmSynthetic
		internal fun create(context: Context): ShizukuPackageInstaller {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm")
			}
			val remotePackageManager = IPackageManager.Stub.asInterface(
				ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
			)
			val remotePackageInstaller = IPackageInstaller.Stub.asInterface(
				ShizukuBinderWrapper(remotePackageManager.packageInstaller.asBinder())
			)
			val uid = Shizuku.getUid()
			val isRoot = uid == 0
			val installerPackageName = if (isRoot) context.packageName else "com.android.shell"
			val userId = if (isRoot) Process.myUserHandle().hashCode() else 0
			return ShizukuPackageInstaller(
				createPackageInstaller(context, remotePackageInstaller, installerPackageName, userId),
				remotePackageInstaller,
				uid
			)
		}

		private fun createPackageInstaller(
			context: Context,
			remotePackageInstaller: IPackageInstaller,
			installerPackageName: String,
			userId: Int
		) = when {
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PackageInstaller::class.java.getConstructor(
				IPackageInstaller::class.java,
				String::class.java,
				String::class.java,
				Int::class.javaPrimitiveType
			).newInstance(remotePackageInstaller, installerPackageName, context.attributionTagCompat, userId)

			Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> PackageInstaller::class.java.getConstructor(
				IPackageInstaller::class.java,
				String::class.java,
				Int::class.javaPrimitiveType
			).newInstance(remotePackageInstaller, installerPackageName, userId)

			else -> context.applicationContext.let { applicationContext ->
				PackageInstaller::class.java.getConstructor(
					Context::class.java,
					PackageManager::class.java,
					IPackageInstaller::class.java,
					String::class.java,
					Int::class.javaPrimitiveType
				).newInstance(
					applicationContext,
					applicationContext.packageManager,
					remotePackageInstaller,
					installerPackageName,
					userId
				)
			}
		}

		private val Context.attributionTagCompat
			get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				attributionTag
			} else {
				null
			}
	}
}