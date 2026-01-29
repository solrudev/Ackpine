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

package ru.solrudev.ackpine.impl.plugability

import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.util.UUID

internal interface TestService : AckpineService
internal interface UnregisteredService : AckpineService

internal class TestServiceImpl : TestService {
	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {}
}

internal class TestPluginParametersStore : PluginParametersStore {
	override fun getForSession(sessionId: UUID): AckpinePlugin.Parameters = AckpinePlugin.Parameters.None
	override fun setForSession(sessionId: UUID, params: AckpinePlugin.Parameters) {}
}

internal class TestAckpineServiceProvider : AbstractAckpineServiceProvider(
	serviceFactories = setOf(
		ServiceFactory(TestService::class) { TestServiceImpl() }
	),
	pluginEntries = setOf(
		PluginEntry(TEST_PLUGIN_ID) { TestPluginParametersStore() }
	)
) {
	internal companion object {
		internal const val TEST_PLUGIN_ID = "test-plugin"
		internal const val PLUGIN_ONE_ID = "plugin-one"
		internal const val PLUGIN_TWO_ID = "plugin-two"
	}
}

internal class TestPlugin : AckpinePlugin<AckpinePlugin.Parameters.None> {
	override val id: String = TestAckpineServiceProvider.TEST_PLUGIN_ID
}

internal class PluginOne : AckpinePlugin<PluginOneParams> {
	override val id: String = TestAckpineServiceProvider.PLUGIN_ONE_ID
}

internal class PluginTwo : AckpinePlugin<PluginTwoParams> {
	override val id: String = TestAckpineServiceProvider.PLUGIN_TWO_ID
}

internal data class PluginOneParams(val value: String) : AckpinePlugin.Parameters
internal data class PluginTwoParams(val value: String) : AckpinePlugin.Parameters