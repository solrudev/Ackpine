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

package ru.solrudev.ackpine.plugability

import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters

class TestPlugin : AckpinePlugin<TestPlugin.Parameters> {

	override val id = "test-plugin"

	override fun apply(builder: InstallParameters.Builder) {
		builder.setName("applied-by-plugin")
	}

	override fun apply(builder: UninstallParameters.Builder) {
		builder.setPackageName("applied-by-plugin")
	}

	data class Parameters(val value: String) : AckpinePlugin.Parameters
}

class TestParameterlessPlugin : AckpinePlugin<AckpinePlugin.Parameters.None> {

	override val id = "test-parameterless-plugin"

	override fun apply(builder: InstallParameters.Builder) {
		builder.setName("applied-by-plugin")
	}

	override fun apply(builder: UninstallParameters.Builder) {
		builder.setPackageName("applied-by-plugin")
	}
}