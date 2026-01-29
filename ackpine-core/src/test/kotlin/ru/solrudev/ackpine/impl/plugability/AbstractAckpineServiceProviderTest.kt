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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class AbstractAckpineServiceProviderTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private lateinit var provider: TestAckpineServiceProvider

	@BeforeTest
	fun setUp() {
		provider = TestAckpineServiceProvider()
		provider.initContext(context)
	}

	@Test
	fun getReturnsServiceWhenRegistered() {
		val service = provider[TestService::class]
		assertNotNull(service)
	}

	@Test
	fun getReturnsNullForUnregisteredService() {
		val service = provider[UnregisteredService::class]
		assertNull(service)
	}

	@Test
	fun getReturnsSameInstanceOnMultipleCalls() {
		val first = provider[TestService::class]
		val second = provider[TestService::class]
		assertSame(first, second)
	}

	@Test
	fun pluginIdentifiersContainsRegisteredPlugins() {
		assertEquals(setOf(TestAckpineServiceProvider.TEST_PLUGIN_ID), provider.pluginIdentifiers)
	}

	@Test
	fun getPluginParametersStoresReturnsRegisteredStores() {
		val stores = provider.getPluginParametersStores()
		assertEquals(1, stores.size)
	}

	@Test
	fun getPluginParametersStoresReturnsSameInstanceOnMultipleCalls() {
		val first = provider.getPluginParametersStores()
		val second = provider.getPluginParametersStores()
		assertSame(first.first(), second.first())
	}
}