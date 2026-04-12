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
import ru.solrudev.ackpine.AckpineThreadPool
import ru.solrudev.ackpine.impl.plugability.AbstractAckpineServiceProvider
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.libsu.database.LibsuDatabase

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class LibsuServiceProvider : AbstractAckpineServiceProvider(
	serviceFactories = setOf(
		ServiceFactory(PackageInstallerService::class, RootPackageInstaller::create)
	),
	pluginEntries = setOf(
		PluginEntry(LibsuPlugin.PLUGIN_ID) { context ->
			val database = LibsuDatabase.getInstance(context, AckpineThreadPool)
			LibsuPluginParametersStore(
				database.libsuInstallParamsDao(),
				database.libsuUninstallParamsDao()
			)
		}
	)
)