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

package ru.solrudev.ackpine.impl.database

import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.model.HasPlugins
import ru.solrudev.ackpine.impl.database.model.HasSession
import ru.solrudev.ackpine.impl.database.model.PluginEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginContainer
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData

@JvmSynthetic
internal fun <F : Failure> HasSession.getState(
	sessionFailureDao: SessionFailureDao<F>
): Session.State<F> {
	return session.state.toSessionState(session.id, sessionFailureDao)
}

@JvmSynthetic
internal fun HasSession.getNotificationData() = NotificationData.Builder()
	.setTitle(session.notificationTitle)
	.setContentText(session.notificationText)
	.setIcon(session.notificationIcon)
	.build()

@Suppress("UNCHECKED_CAST")
@JvmSynthetic
internal fun HasPlugins.getPlugins(): List<Class<out AckpinePlugin<*>>> {
	return plugins.map { pluginEntity ->
		Class.forName(pluginEntity.pluginClassName) as Class<out AckpinePlugin<*>>
	}
}

@JvmSynthetic
internal fun AckpinePluginContainer.toEntityList(sessionId: String): List<PluginEntity> {
	return getPlugins().map { (pluginClass, _) ->
		PluginEntity(
			sessionId = sessionId,
			pluginClassName = pluginClass.name
		)
	}
}

private fun <F : Failure> SessionEntity.State.toSessionState(
	id: String,
	sessionFailureDao: SessionFailureDao<F>
): Session.State<F> = when (this) {
	SessionEntity.State.PENDING -> Session.State.Pending
	SessionEntity.State.ACTIVE -> Session.State.Active
	SessionEntity.State.AWAITING -> Session.State.Awaiting
	SessionEntity.State.COMMITTED -> Session.State.Committed
	SessionEntity.State.CANCELLED -> Session.State.Cancelled
	SessionEntity.State.SUCCEEDED -> Session.State.Succeeded
	SessionEntity.State.FAILED -> {
		val failure = sessionFailureDao.getFailure(id)
		Session.State.Failed(failure!!)
	}
}