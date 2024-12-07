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

package ru.solrudev.ackpine.sample.updater

import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Progress

data class UpdaterUiState(
	val isInstalling: Boolean = false,
	val progress: Progress = Progress(),
	val error: ResolvableString = ResolvableString.empty(),
	val isCancellable: Boolean = true,
	val buttonText: ResolvableString = ResolvableString.transientResource(R.string.install)
)