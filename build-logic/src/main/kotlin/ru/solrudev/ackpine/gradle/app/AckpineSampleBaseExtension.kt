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

package ru.solrudev.ackpine.gradle.app

import com.android.build.api.dsl.ApplicationExtension
import ru.solrudev.ackpine.gradle.AckpineCommonExtension
import ru.solrudev.ackpine.gradle.SampleConstants
import javax.inject.Inject

/**
 * Extension for Ackpine `sample` plugin.
 */
public abstract class AckpineSampleBaseExtension @Inject constructor(
	private val applicationExtension: ApplicationExtension
) : AckpineCommonExtension(applicationExtension, SampleConstants.PACKAGE_NAME) {

	override var id: String
		get() = super.id
		set(value) {
			super.id = value
			applicationExtension.defaultConfig.applicationId = applicationExtension.namespace
		}
}