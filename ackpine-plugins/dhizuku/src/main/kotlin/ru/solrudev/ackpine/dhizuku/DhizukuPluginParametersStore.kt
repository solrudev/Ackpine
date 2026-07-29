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

package ru.solrudev.ackpine.dhizuku

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.dhizuku.database.DhizukuInstallParametersEntity
import ru.solrudev.ackpine.dhizuku.database.DhizukuInstallParamsDao
import ru.solrudev.ackpine.dhizuku.database.DhizukuUninstallParametersEntity
import ru.solrudev.ackpine.dhizuku.database.DhizukuUninstallParamsDao
import ru.solrudev.ackpine.impl.plugability.PluginParametersStore
import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class DhizukuPluginParametersStore(
	private val dhizukuInstallParamsDao: DhizukuInstallParamsDao,
	private val dhizukuUninstallParamsDao: DhizukuUninstallParamsDao
) : PluginParametersStore {

	override fun getForSession(sessionId: UUID): AckpinePlugin.Parameters {
		val id = sessionId.toString()
		dhizukuInstallParamsDao.getBySessionId(id)?.let { dhizukuParams ->
			return DhizukuPlugin.InstallParameters.Builder()
				.setRequestDowngrade(dhizukuParams.requestDowngrade)
				.build()
		}
		dhizukuUninstallParamsDao.getBySessionId(id)?.let { dhizukuParams ->
			return DhizukuPlugin.UninstallParameters.Builder()
				.setKeepData(dhizukuParams.keepData)
				.setAllUsers(dhizukuParams.allUsers)
				.setSystemApp(dhizukuParams.systemApp)
				.build()
		}
		return AckpinePlugin.Parameters.None
	}

	override fun setForSession(sessionId: UUID, params: AckpinePlugin.Parameters) = when (params) {
		is DhizukuPlugin.InstallParameters -> {
			val dhizukuParams = DhizukuInstallParametersEntity(
				sessionId = sessionId.toString(),
				requestDowngrade = params.requestDowngrade
			)
			dhizukuInstallParamsDao.insertParameters(dhizukuParams)
		}

		is DhizukuPlugin.UninstallParameters -> {
			val dhizukuParams = DhizukuUninstallParametersEntity(
				sessionId = sessionId.toString(),
				keepData = params.keepData,
				allUsers = params.allUsers,
				systemApp = params.systemApp
			)
			dhizukuUninstallParamsDao.insertParameters(dhizukuParams)
		}

		else -> {
		}
	}
}