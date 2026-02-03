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

package ru.solrudev.ackpine.impl.testutil

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter
import ru.solrudev.ackpine.impl.AndroidTv
import ru.solrudev.ackpine.impl.ExcludeAndroidTv
import ru.solrudev.ackpine.impl.OptInAndroid11

@Suppress("Unused")
class DeviceAwareTestFilter : Filter() {

	private val context = InstrumentationRegistry.getInstrumentation().targetContext

	override fun shouldRun(description: Description): Boolean {
		val testClass = description.testClass ?: return false
		if (description.isSuite) {
			return when {
				testClass.isAnnotationPresent(OptInAndroid11::class.java) && isAndroid11() -> true
				testClass.isAnnotationPresent(AndroidTv::class.java) && !context.isTv() -> false
				testClass.isAnnotationPresent(ExcludeAndroidTv::class.java) && context.isTv() -> false
				else -> description.children.isEmpty() || description.children.any(::shouldRun)
			}
		}
		val annotations = description.annotations
		return when {
			!hasAnnotation<OptInAndroid11>(annotations, testClass) && isAndroid11() -> false
			hasAnnotation<AndroidTv>(annotations, testClass) && !context.isTv() -> false
			hasAnnotation<ExcludeAndroidTv>(annotations, testClass) && context.isTv() -> false
			else -> true
		}
	}

	override fun describe() = "DeviceAwareTestFilter"

	private inline fun <reified T : Annotation> hasAnnotation(
		methodAnnotations: Collection<Annotation>,
		testClass: Class<*>
	): Boolean {
		return methodAnnotations.any { it is T } || testClass.isAnnotationPresent(T::class.java)
	}
}