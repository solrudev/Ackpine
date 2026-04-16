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

import ru.solrudev.ackpine.AckpineLogger

internal class RecordingAckpineLogger : AckpineLogger {

	private val _events = mutableListOf<Event>()
	val events: List<Event> = _events

	override fun log(
		level: AckpineLogger.Level,
		tag: String,
		template: String,
		throwable: Throwable?,
		args: Array<out Any?>
	) {
		_events += Event(level, tag, template, throwable, args.toList())
	}

	fun lastEvent() = events.last()

	fun contains(templateSubstring: String, tag: String? = null): Boolean {
		return events.any { event ->
			templateSubstring in event.messageTemplate && (tag == null || event.tag == tag)
		}
	}

	internal data class Event(
		val level: AckpineLogger.Level,
		val tag: String,
		val messageTemplate: String,
		val throwable: Throwable?,
		val args: List<Any?>
	)
}