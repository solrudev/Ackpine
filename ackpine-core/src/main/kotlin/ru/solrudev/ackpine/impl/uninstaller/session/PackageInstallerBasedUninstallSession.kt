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

package ru.solrudev.ackpine.impl.uninstaller.session

import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.createPackageInstallerStatusIntentSender
import ru.solrudev.ackpine.impl.logging.AckpineLoggerProvider
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.impl.session.AbstractSession
import ru.solrudev.ackpine.impl.uninstaller.UninstallStatusReceiver
import ru.solrudev.ackpine.impl.uninstaller.activity.UninstallActivity
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.random.Random
import kotlin.random.nextInt

private const val TAG = "PackageInstallerBasedUninstallSession"

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PackageInstallerBasedUninstallSession internal constructor(
	loggerProvider: AckpineLoggerProvider,
	private val context: Context,
	packageInstallerService: Lazy<PackageInstallerService>,
	private val packageName: String,
	id: UUID,
	initialState: Session.State<UninstallFailure>,
	private val confirmation: Confirmation,
	private val notificationData: NotificationData,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<UninstallFailure>,
	executor: Executor,
	handler: Handler,
	notificationId: Int,
	dbWriteSemaphore: BinarySemaphore
) : AbstractSession<UninstallFailure>(
	context, loggerProvider.withTag(TAG), id, initialState,
	sessionDao, sessionFailureDao,
	executor, handler,
	exceptionalFailureFactory = UninstallFailure::Exceptional,
	notificationId, dbWriteSemaphore
) {

	private val packageInstaller by packageInstallerService

	override fun prepare() {
		// no preparation needed
		notifyAwaiting()
	}

	override fun launchConfirmation() {
		logger.debug(
			"Launching package installer uninstall for session %s packageName=%s",
			id,
			packageName
		)
		packageInstaller.uninstall(
			packageName,
			statusReceiver = createPackageInstallerStatusIntentSender(),
			ackpineSessionId = id
		)
	}

	private fun createPackageInstallerStatusIntentSender(): IntentSender {
		return createPackageInstallerStatusIntentSender<UninstallStatusReceiver>(
			context,
			action = UninstallStatusReceiver.getAction(context),
			sessionId = id,
			confirmation, notificationId, notificationData, generateRequestCode()
		) { intent ->
			intent.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, packageName)
		}
	}

	private fun generateRequestCode() = Random.nextInt(4000000..5000000)
}