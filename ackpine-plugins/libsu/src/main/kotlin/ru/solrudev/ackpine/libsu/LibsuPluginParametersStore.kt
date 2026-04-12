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

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.plugability.PluginParametersStore
import ru.solrudev.ackpine.libsu.database.LibsuInstallParametersEntity
import ru.solrudev.ackpine.libsu.database.LibsuInstallParamsDao
import ru.solrudev.ackpine.libsu.database.LibsuUninstallParametersEntity
import ru.solrudev.ackpine.libsu.database.LibsuUninstallParamsDao
import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class LibsuPluginParametersStore(
	private val libsuInstallParamsDao: LibsuInstallParamsDao,
	private val libsuUninstallParamsDao: LibsuUninstallParamsDao
) : PluginParametersStore {

	override fun getForSession(sessionId: UUID): AckpinePlugin.Parameters {
		val id = sessionId.toString()
		libsuInstallParamsDao.getBySessionId(id)?.let { libsuParams ->
			return LibsuPlugin.InstallParameters.Builder()
				.setBypassLowTargetSdkBlock(libsuParams.bypassLowTargetSdkBlock)
				.setAllowTest(libsuParams.allowTest)
				.setReplaceExisting(libsuParams.replaceExisting)
				.setRequestDowngrade(libsuParams.requestDowngrade)
				.setGrantAllRequestedPermissions(libsuParams.grantAllRequestedPermissions)
				.setAllUsers(libsuParams.allUsers)
				.setInstallerPackageName(libsuParams.installerPackageName)
				.build()
		}
		libsuUninstallParamsDao.getBySessionId(id)?.let { libsuParams ->
			return LibsuPlugin.UninstallParameters.Builder()
				.setKeepData(libsuParams.keepData)
				.setAllUsers(libsuParams.allUsers)
				.build()
		}
		return AckpinePlugin.Parameters.None
	}

	override fun setForSession(sessionId: UUID, params: AckpinePlugin.Parameters) = when (params) {
		is LibsuPlugin.InstallParameters -> {
			val libsuParams = LibsuInstallParametersEntity(
				sessionId = sessionId.toString(),
				bypassLowTargetSdkBlock = params.bypassLowTargetSdkBlock,
				allowTest = params.allowTest,
				replaceExisting = params.replaceExisting,
				requestDowngrade = params.requestDowngrade,
				grantAllRequestedPermissions = params.grantAllRequestedPermissions,
				allUsers = params.allUsers,
				installerPackageName = params.installerPackageName
			)
			libsuInstallParamsDao.insertParameters(libsuParams)
		}

		is LibsuPlugin.UninstallParameters -> {
			val libsuParams = LibsuUninstallParametersEntity(
				sessionId = sessionId.toString(),
				keepData = params.keepData,
				allUsers = params.allUsers
			)
			libsuUninstallParamsDao.insertParameters(libsuParams)
		}

		else -> { // ignore
		}
	}
}