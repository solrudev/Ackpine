/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.installer.session

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval

/**
 * A listener for [pre-commit install preapproval][InstallPreapproval].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface PreapprovalListener {

	/**
	 * Called when preapproval is requested.
	 */
	fun onPreapprovalStarted()

	/**
	 * Called when session preapproval has been completed successfully.
	 */
	fun onPreapprovalSucceeded()

	/**
	 * Called when session preapproval has been completed with failure.
	 * @param status internal package manager status, representing exact install failure reason.
	 * @param publicFailure an [InstallFailure] instance to which the [status] is mapped.
	 */
	fun onPreapprovalFailed(
		status: PackageInstallerStatus?,
		publicFailure: InstallFailure
	)
}