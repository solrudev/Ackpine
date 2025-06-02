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
import androidx.annotation.RestrictTo
import rikka.shizuku.Shizuku
import ru.solrudev.ackpine.impl.installer.PackageInstallerService
import ru.solrudev.ackpine.impl.plugability.AbstractAckpineServiceProvider
import ru.solrudev.ackpine.impl.plugability.AckpineService
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ShizukuServiceProvider : AbstractAckpineServiceProvider(
	serviceFactories = setOf(
		ServiceFactory(PackageInstallerService::class, ShizukuPackageInstaller::create)
	)
) {
	override val pluginId = ShizukuPlugin.PLUGIN_ID

	override fun <T : AckpineService> get(serviceClass: KClass<T>, context: Context): T? {
		if (Shizuku.isPreV11()) {
			return null
		}
		return super.get(serviceClass, context)
	}
}