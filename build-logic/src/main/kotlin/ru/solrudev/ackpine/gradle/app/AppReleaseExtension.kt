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

package ru.solrudev.ackpine.gradle.app

import org.gradle.api.provider.Property

/**
 * Extension for Ackpine `app-release` plugin.
 */
public interface AppReleaseExtension {

	/**
	 * Enables bundle extraction. When `true`, the plugin builds an app bundle, extracts split APKs
	 * using `bundletool`, and publishes them through `ackpineAppElements`.
	 */
	public val publishSplits: Property<Boolean>
}