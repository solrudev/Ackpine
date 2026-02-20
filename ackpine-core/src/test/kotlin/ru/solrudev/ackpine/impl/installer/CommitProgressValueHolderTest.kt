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

package ru.solrudev.ackpine.impl.installer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CommitProgressValueHolderTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@BeforeTest
	fun setUp() {
		CommitProgressValueHolder.clear(context)
	}

	@Test
	fun getReturnsDefaultWhenNotSet() {
		val value = CommitProgressValueHolder.get(context)
		assertEquals(1f, value)
	}

	@Test
	fun putIfAbsentStoresValue() {
		CommitProgressValueHolder.putIfAbsent(context) { 0.42f }
		val value = CommitProgressValueHolder.get(context)
		assertEquals(0.42f, value)
	}

	@Test
	fun putIfAbsentDoesNotOverwriteExistingValue() {
		CommitProgressValueHolder.putIfAbsent(context) { 0.5f }
		CommitProgressValueHolder.putIfAbsent(context) { 0.8f }
		val value = CommitProgressValueHolder.get(context)
		assertEquals(0.5f, value)
	}

	@Test
	fun getAsyncReturnsStoredValue() {
		CommitProgressValueHolder.putIfAbsent(context) { 0.42f }
		val value = CommitProgressValueHolder.getAsync(context).get()
		assertEquals(0.42f, value)
	}

	@Test
	fun getAsyncReturnsCachedValueWithoutReading() {
		CommitProgressValueHolder.putIfAbsent(context) { 0.42f }
		val cached = CommitProgressValueHolder.get(context)
		CommitProgressValueHolder.clearPreferences(context)
		val value = CommitProgressValueHolder.getAsync(context).get()
		assertEquals(cached, value)
	}

	@Test
	fun getReturnsCachedValue() {
		CommitProgressValueHolder.putIfAbsent(context) { 0.42f }
		val cached = CommitProgressValueHolder.get(context)
		CommitProgressValueHolder.clearPreferences(context)
		val value = CommitProgressValueHolder.get(context)
		assertEquals(cached, value)
	}
}