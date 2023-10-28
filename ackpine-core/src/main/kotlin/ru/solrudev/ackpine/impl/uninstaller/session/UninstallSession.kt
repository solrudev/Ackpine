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

package ru.solrudev.ackpine.impl.uninstaller.session

import android.content.Context
import android.os.CancellationSignal
import android.os.Handler
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.session.AbstractSession
import ru.solrudev.ackpine.impl.session.globalNotificationId
import ru.solrudev.ackpine.impl.session.helpers.UPDATE_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.session.helpers.launchConfirmation
import ru.solrudev.ackpine.impl.uninstaller.activity.UninstallActivity
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.random.Random

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class UninstallSession internal constructor(
	private val context: Context,
	private val packageName: String,
	id: UUID,
	initialState: Session.State<UninstallFailure>,
	private val confirmation: Confirmation,
	private val notificationData: NotificationData,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<UninstallFailure>,
	notificationIdDao: NotificationIdDao,
	serialExecutor: Executor,
	handler: Handler,
	newNotificationId: Int = globalNotificationId.incrementAndGet()
) : AbstractSession<UninstallFailure>(
	context, UNINSTALLER_NOTIFICATION_TAG,
	id, initialState,
	sessionDao, sessionFailureDao, notificationIdDao,
	serialExecutor, handler,
	exceptionalFailureFactory = UninstallFailure::Exceptional,
	newNotificationId
) {

	override fun prepare(cancellationSignal: CancellationSignal) {
		// no preparation needed
		notifyAwaiting()
	}

	override fun launchConfirmation(cancellationSignal: CancellationSignal, notificationId: Int) {
		context.launchConfirmation<UninstallActivity>(
			confirmation, notificationData,
			sessionId = id,
			UNINSTALLER_NOTIFICATION_TAG, notificationId,
			generateRequestCode(),
			UPDATE_CURRENT_FLAGS
		) { intent -> intent.putExtra(UninstallActivity.PACKAGE_NAME_KEY, packageName) }
	}

	private fun generateRequestCode() = Random.nextInt(from = 3000000, until = 4000000)
}