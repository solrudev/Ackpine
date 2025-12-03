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

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.plugability.PluginParametersStore
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.shizuku.database.ShizukuParametersEntity
import ru.solrudev.ackpine.shizuku.database.ShizukuParamsDao
import ru.solrudev.ackpine.shizuku.database.ShizukuUninstallParametersEntity
import ru.solrudev.ackpine.shizuku.database.ShizukuUninstallParamsDao
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ShizukuPluginParametersStore(
	private val shizukuParamsDao: ShizukuParamsDao
) : PluginParametersStore {

	override fun getForSession(sessionId: UUID): AckpinePlugin.Parameters {
		val shizukuParams = shizukuParamsDao.getBySessionId(sessionId.toString())
		return ShizukuPlugin.Parameters.Builder()
			.setBypassLowTargetSdkBlock(shizukuParams.bypassLowTargetSdkBlock)
			.setAllowTest(shizukuParams.allowTest)
			.setReplaceExisting(shizukuParams.replaceExisting)
			.setRequestDowngrade(shizukuParams.requestDowngrade)
			.setGrantAllRequestedPermissions(shizukuParams.grantAllRequestedPermissions)
			.setAllUsers(shizukuParams.allUsers)
			.build()
	}

	override fun setForSession(
		sessionId: UUID,
		params: AckpinePlugin.Parameters
	) {
		if (params !is ShizukuPlugin.Parameters) {
			return
		}
		val shizukuParams = ShizukuParametersEntity(
			sessionId = sessionId.toString(),
			bypassLowTargetSdkBlock = params.bypassLowTargetSdkBlock,
			allowTest = params.allowTest,
			replaceExisting = params.replaceExisting,
			requestDowngrade = params.requestDowngrade,
			grantAllRequestedPermissions = params.grantAllRequestedPermissions,
			allUsers = params.allUsers
		)
		shizukuParamsDao.insertParameters(shizukuParams)
	}
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ShizukuUninstallPluginParametersStore(
	private val shizukuParamsDao: ShizukuUninstallParamsDao
) : PluginParametersStore {

	override fun getForSession(sessionId: UUID): AckpinePlugin.Parameters {
		val shizukuParams = shizukuParamsDao.getBySessionId(sessionId.toString())
		return ShizukuUninstallPlugin.Parameters.Builder()
			.setKeepData(shizukuParams.keepData)
			.setAllUsers(shizukuParams.allUsers)
			.build()
	}

	override fun setForSession(
		sessionId: UUID,
		params: AckpinePlugin.Parameters
	) {
		if (params !is ShizukuUninstallPlugin.Parameters) {
			return
		}
		val shizukuParams = ShizukuUninstallParametersEntity(
			sessionId = sessionId.toString(),
			keepData = params.keepData,
			allUsers = params.allUsers
		)
		shizukuParamsDao.insertParameters(shizukuParams)
	}
}