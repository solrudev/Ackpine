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

package ru.solrudev.ackpine.impl.services

import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Handler
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.plugability.AckpineService
import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.io.Closeable
import java.io.OutputStream
import java.util.UUID

/**
 * Provides functionality of Android's [android.content.pm.PackageInstaller].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public interface PackageInstallerService : AckpineService {

	/**
	 * UID of the process owning underlying package installer service.
	 */
	public val uid: Int

	/**
	 * @param ackpineSessionId ID of the Ackpine install session.
	 * @see android.content.pm.PackageInstaller.createSession
	 */
	public fun createSession(params: PackageInstaller.SessionParams, ackpineSessionId: UUID): Int

	/**
	 * @see PackageInstaller.openSession
	 */
	public fun openSession(sessionId: Int): Session

	/**
	 * @see PackageInstaller.getSessionInfo
	 */
	public fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo?

	/**
	 * @see PackageInstaller.commitSessionAfterInstallConstraintsAreMet
	 */
	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	public fun commitSessionAfterInstallConstraintsAreMet(
		sessionId: Int,
		statusReceiver: IntentSender,
		constraints: PackageInstaller.InstallConstraints,
		timeoutMillis: Long
	)

	/**
	 * @see PackageInstaller.registerSessionCallback
	 */
	public fun registerSessionCallback(callback: PackageInstaller.SessionCallback, handler: Handler)

	/**
	 * @see PackageInstaller.unregisterSessionCallback
	 */
	public fun unregisterSessionCallback(callback: PackageInstaller.SessionCallback)

	/**
	 * @see PackageInstaller.abandonSession
	 */
	public fun abandonSession(sessionId: Int)

	/**
	 * @see [PackageInstaller.uninstall]
	 */
	public fun uninstall(packageName: String, statusReceiver: IntentSender, ackpineSessionId: UUID)

	/**
	 * A facade for [PackageInstaller.Session].
	 */
	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public interface Session : Closeable {

		/**
		 * @see PackageInstaller.Session.openWrite
		 */
		public fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream

		/**
		 * @see PackageInstaller.Session.fsync
		 */
		public fun fsync(out: OutputStream)

		/**
		 * @see PackageInstaller.Session.setStagingProgress
		 */
		public fun setStagingProgress(progress: Float)

		/**
		 * @see PackageInstaller.Session.commit
		 */
		public fun commit(statusReceiver: IntentSender)

		/**
		 * @see PackageInstaller.Session.requestUserPreapproval
		 */
		@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
		public fun requestUserPreapproval(details: PackageInstaller.PreapprovalDetails, statusReceiver: IntentSender)
	}
}

/**
 * A [PackageInstallerService.Session] implementation which forwards all calls to the underlying
 * [PackageInstaller.Session].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class PackageInstallerSessionWrapper(private val session: PackageInstaller.Session) :
	PackageInstallerService.Session {
	override fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream =
		session.openWrite(name, offsetBytes, lengthBytes)

	override fun fsync(out: OutputStream): Unit = session.fsync(out)
	override fun setStagingProgress(progress: Float): Unit = session.setStagingProgress(progress)
	override fun commit(statusReceiver: IntentSender): Unit = session.commit(statusReceiver)

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	override fun requestUserPreapproval(
		details: PackageInstaller.PreapprovalDetails,
		statusReceiver: IntentSender
	): Unit = session.requestUserPreapproval(details, statusReceiver)

	override fun close(): Unit = session.close()
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PackageInstallerWrapper(
	private val packageInstaller: PackageInstaller,
	override val uid: Int
) : PackageInstallerService {

	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) { // no-op
	}

	override fun createSession(params: PackageInstaller.SessionParams, ackpineSessionId: UUID) =
		packageInstaller.createSession(params)

	override fun openSession(sessionId: Int) = PackageInstallerSessionWrapper(packageInstaller.openSession(sessionId))
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

	override fun abandonSession(sessionId: Int) = packageInstaller.abandonSession(sessionId)

	override fun uninstall(packageName: String, statusReceiver: IntentSender, ackpineSessionId: UUID) =
		packageInstaller.uninstall(packageName, statusReceiver)

	internal companion object {
		@JvmSynthetic
		internal fun default(context: Context) = lazy {
			PackageInstallerWrapper(context.packageManager.packageInstaller, Process.myUid())
		}
	}
}