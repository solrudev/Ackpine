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

package ru.solrudev.ackpine.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.solrudev.ackpine.gradle.helpers.readProperties

internal abstract class PropertiesFileService : BuildService<PropertiesFileService.Parameters> {

	internal val properties by lazy {
		val file = parameters.propertiesFile.get().asFile
		if (file.exists()) {
			file.readProperties()
		} else {
			emptyMap()
		}
	}

	internal interface Parameters : BuildServiceParameters {
		val propertiesFile: RegularFileProperty
	}
}