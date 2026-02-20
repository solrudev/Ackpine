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
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.test.TestPackageInstaller
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class PackageInstallerImplBridgeTest {

	@Test
	fun getInstanceReturnsSingletonBridgeImplementation() {
		val first = PackageInstaller.getInstance(Context)
		val second = PackageInstaller.getInstance(Context)
		assertSame(first, second)
		assertIs<TestPackageInstaller>(first)
	}
}