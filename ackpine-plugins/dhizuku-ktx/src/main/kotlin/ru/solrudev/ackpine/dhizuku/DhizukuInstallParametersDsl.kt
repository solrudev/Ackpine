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

import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

/**
 * DSL allowing to configure [install parameters for DhizukuPlugin][DhizukuPlugin.InstallParameters].
 */
@SessionParametersDsl
public interface DhizukuInstallParametersDsl {

	/**
	 * Whether an upgrade to a lower version of a package than currently installed is requested.
	 *
	 * Ackpine sets downgrade-request flag which is honored for debuggable installed apps or debuggable OS builds, but
	 * does not add the unrestricted downgrade permission flag used by root, shell, or system identities.
	 * The device owner must still be authorized by Android to perform the downgrade, so setting this option does
	 * not guarantee that a downgrade will succeed.
	 *
	 * This is a **delicate** API. The outcome of using this flag depends on the app being installed, on a
	 * specific OS build, and on the permissions that the used device owner identity has.
	 */
	@set:DelicateAckpineApi
	public var requestDowngrade: Boolean
}

@PublishedApi
internal class DhizukuInstallParametersDslBuilder : DhizukuInstallParametersDsl {

	private val delegate = DhizukuPlugin.InstallParameters.Builder()

	@set:DelicateAckpineApi
	override var requestDowngrade: Boolean
		get() = delegate.requestDowngrade
		set(value) {
			delegate.setRequestDowngrade(value)
		}

	fun build(): DhizukuPlugin.InstallParameters = delegate.build()
}