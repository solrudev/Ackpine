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

package ru.solrudev.ackpine.gradle.testing

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HasDeviceTestsBuilder
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.VariantBuilder
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal inline fun <reified E : AndroidComponentsExtension<*, VB, *>, VB> Project.configureTests(
	testing: AckpineTestingOptions
) where VB : VariantBuilder,
		VB : HasHostTestsBuilder,
		VB : HasDeviceTestsBuilder {
	extensions.configure<E> {
		beforeVariants { variantBuilder ->
			variantBuilder
				.hostTests
				.getValue(HostTestBuilder.UNIT_TEST_TYPE)
				.enable = testing.enableHostTests.get() && variantBuilder.buildType == "debug"
			variantBuilder
				.deviceTests
				.getValue(DeviceTestBuilder.ANDROID_TEST_TYPE)
				.enable = testing.enableDeviceTests.get() && variantBuilder.buildType == "debug"
		}
	}
}