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
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.impl.plugability.AbstractAckpineServiceProvider.PluginEntry
import ru.solrudev.ackpine.impl.plugability.AbstractAckpineServiceProvider.ServiceFactory
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.impl.testutil.TestFailure
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class AckpineServiceProvidersTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private lateinit var testProvider: TestAckpineServiceProvider

	@BeforeTest
	fun setUp() {
		testProvider = TestAckpineServiceProvider()
		testProvider.initContext(context)
	}

	@Test
	fun getAllReturnsAllRegisteredProviders() {
		val providers = AckpineServiceProviders(lazy { setOf(testProvider) })
		val all = providers.getAll()
		assertEquals(setOf(testProvider), all)
	}

	@Test
	fun createSessionWithServiceUsesDefaultWhenNoProviders() {
		val providers = AckpineServiceProviders(lazy { emptySet() })
		val defaultService = TestServiceImpl()
		val sessionId = UUID.randomUUID()
		var usedService: TestService? = null

		providers.createSessionWithService(
			serviceClass = TestService::class,
			defaultService = lazy { defaultService },
			sessionId = sessionId,
			pluginClasses = Result.success(listOf(TestPlugin::class.java)),
			sessionFactory = { service ->
				usedService = service
				DummyCompletableSession(sessionId)
			}
		)

		assertSame(defaultService, usedService)
	}

	@Test
	fun createSessionWithServiceUsesDefaultWhenNoPluginsApplied() {
		val providers = AckpineServiceProviders(lazy { setOf(testProvider) })
		val defaultService = TestServiceImpl()
		val sessionId = UUID.randomUUID()
		var usedService: TestService? = null

		providers.createSessionWithService(
			serviceClass = TestService::class,
			defaultService = lazy { defaultService },
			sessionId = sessionId,
			pluginClasses = Result.success(emptyList()),
			sessionFactory = { service ->
				usedService = service
				DummyCompletableSession(sessionId)
			}
		)

		assertSame(defaultService, usedService)
	}

	@Test
	fun createSessionWithServiceCompletesSessionExceptionallyOnPluginClassesError() {
		val providers = AckpineServiceProviders(lazy { emptySet() })
		val sessionId = UUID.randomUUID()
		val exception = RuntimeException("plugin error")

		val session = providers.createSessionWithService(
			serviceClass = TestService::class,
			defaultService = lazy { TestServiceImpl() },
			sessionId = sessionId,
			pluginClasses = Result.failure(exception),
			sessionFactory = { DummyCompletableSession(sessionId) }
		)

		assertSame(exception, session.completionException)
	}

	@Test
	fun getByPluginsReturnsProvidersWithMatchingPluginId() {
		val providers = AckpineServiceProviders(lazy { setOf(testProvider) })
		val result = providers.getByPlugins(listOf(TestPlugin::class.java))
		assertEquals(listOf(testProvider), result)
	}

	@Test
	fun createSessionWithServiceUsesProviderServiceAndAppliesMatchingParameters() {
		val sessionId = UUID.randomUUID()
		val pluginService = RecordingTestService()
		val testParams = TestParams(value = "value")
		val otherParams = PluginOneParams(value = "other value")
		val provider = FakeAckpineServiceProvider(
			serviceFactories = setOf(ServiceFactory(TestService::class) { pluginService }),
			pluginEntries = setOf(
				PluginEntry(TestAckpineServiceProvider.TEST_PLUGIN_ID) { FakePluginParametersStore(testParams) },
				PluginEntry(TestAckpineServiceProvider.PLUGIN_ONE_ID) { FakePluginParametersStore(otherParams) }
			)
		)
		provider.initContext(context)
		val providers = AckpineServiceProviders(lazy { setOf(provider) })
		var usedService: TestService? = null

		val session = providers.createSessionWithService(
			serviceClass = TestService::class,
			defaultService = lazy { TestServiceImpl() },
			sessionId = sessionId,
			pluginClasses = Result.success(listOf(TestPlugin::class.java, PluginOne::class.java)),
			sessionFactory = { service ->
				usedService = service
				DummyCompletableSession(sessionId)
			}
		)

		assertSame(pluginService, usedService)
		val expectedAppliedParameters = listOf(AppliedParameters(sessionId, testParams))
		assertEquals(expectedAppliedParameters, pluginService.appliedParameters)
		assertNull(session.completionException)
	}

	@Test
	fun createSessionWithServiceUsesDefaultWhenNoProviderHasRequestedService() {
		val sessionId = UUID.randomUUID()
		val provider = FakeAckpineServiceProvider(
			pluginEntries = setOf(
				PluginEntry(TestAckpineServiceProvider.TEST_PLUGIN_ID) { TestPluginParametersStore() }
			)
		)
		provider.initContext(context)
		val providers = AckpineServiceProviders(lazy { setOf(provider) })
		val defaultService = TestServiceImpl()
		var usedService: TestService? = null

		providers.createSessionWithService(
			serviceClass = TestService::class,
			defaultService = lazy { defaultService },
			sessionId = sessionId,
			pluginClasses = Result.success(listOf(TestPlugin::class.java)),
			sessionFactory = { service ->
				usedService = service
				DummyCompletableSession(sessionId)
			}
		)

		assertSame(defaultService, usedService)
	}

	@Test
	fun createSessionWithServiceWrapsErrorsFromPluginDiscovery() {
		val providers = AckpineServiceProviders(lazy { emptySet() })
		val sessionId = UUID.randomUUID()

		val session = providers.createSessionWithService(
			serviceClass = TestService::class,
			defaultService = lazy { TestServiceImpl() },
			sessionId = sessionId,
			pluginClasses = Result.failure(AssertionError("plugin error")),
			sessionFactory = { DummyCompletableSession(sessionId) }
		)

		val exception = session.completionException
		assertNotNull(exception)
		assertIs<RuntimeException>(exception)
		assertIs<AssertionError>(exception.cause)
	}

	@Test
	fun persistPluginParametersWritesToAllStores() {
		val sessionId = UUID.randomUUID()
		val store1 = FakePluginParametersStore()
		val store2 = FakePluginParametersStore()
		val provider = FakeAckpineServiceProvider(
			pluginEntries = setOf(
				PluginEntry(TestAckpineServiceProvider.PLUGIN_ONE_ID) { store1 },
				PluginEntry(TestAckpineServiceProvider.PLUGIN_TWO_ID) { store2 }
			)
		)
		provider.initContext(context)
		val providers = AckpineServiceProviders(lazy { setOf(provider) })
		val params1 = PluginOneParams(value = "p1")
		val params2 = PluginTwoParams(value = "p2")
		val installParameters = InstallParameters(Uri.EMPTY) {
			usePlugin(PluginOne::class, params1)
			usePlugin(PluginTwo::class, params2)
		}

		providers.persistPluginParameters(sessionId, installParameters.pluginContainer)

		val expectedParams = setOf(
			StoredPluginParameters(sessionId, params1),
			StoredPluginParameters(sessionId, params2)
		)
		assertEquals(expectedParams, store1.recorded.toSet())
		assertEquals(expectedParams, store2.recorded.toSet())
		assertEquals(2, store1.recorded.size)
		assertEquals(2, store2.recorded.size)
	}

	private class RecordingTestService : TestService {

		private val _appliedParameters = mutableListOf<AppliedParameters<TestParams>>()
		val appliedParameters: List<AppliedParameters<TestParams>> = _appliedParameters

		override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
			if (parameters is TestParams) {
				_appliedParameters += AppliedParameters(sessionId, parameters)
			}
		}
	}

	private data class AppliedParameters<out P : AckpinePlugin.Parameters>(
		val sessionId: UUID,
		val parameters: P
	)

	private data class TestParams(val value: String) : AckpinePlugin.Parameters

	private class FakeAckpineServiceProvider(
		serviceFactories: Set<ServiceFactory<*>> = emptySet(),
		pluginEntries: Set<PluginEntry> = emptySet()
	) : AbstractAckpineServiceProvider(serviceFactories, pluginEntries)

	private class FakePluginParametersStore(
		private val params: AckpinePlugin.Parameters? = null
	) : PluginParametersStore {

		private val _recorded = mutableListOf<StoredPluginParameters>()
		val recorded: List<StoredPluginParameters> get() = _recorded

		override fun getForSession(sessionId: UUID): AckpinePlugin.Parameters {
			return params ?: throw UnsupportedOperationException()
		}

		override fun setForSession(sessionId: UUID, params: AckpinePlugin.Parameters) {
			_recorded += StoredPluginParameters(sessionId, params)
		}
	}

	private data class StoredPluginParameters(
		val sessionId: UUID,
		val parameters: AckpinePlugin.Parameters
	)

	private class DummyCompletableSession(
		override val id: UUID
	) : CompletableSession<TestFailure> {

		var completionException: Exception? = null
			private set

		override val isActive: Boolean = false
		override val isCompleted: Boolean = false
		override val isCancelled: Boolean = false

		override fun launch() = true
		override fun commit() = true

		override fun cancel() { // no-op
		}

		override fun addStateListener(
			subscriptionContainer: DisposableSubscriptionContainer,
			listener: Session.StateListener<TestFailure>
		) = DummyDisposableSubscription

		override fun removeStateListener(listener: Session.StateListener<TestFailure>) { // no-op
		}

		override fun complete(state: Session.State.Completed<TestFailure>) { // no-op
		}

		override fun completeExceptionally(exception: Exception) {
			completionException = exception
		}

		override fun notifyCommitted() { // no-op
		}
	}
}