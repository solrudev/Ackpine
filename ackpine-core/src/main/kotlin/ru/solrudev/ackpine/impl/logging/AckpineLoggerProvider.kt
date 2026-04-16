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

package ru.solrudev.ackpine.impl.logging

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import ru.solrudev.ackpine.AckpineLogger
import ru.solrudev.ackpine.AckpineLogger.Level

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AckpineLoggerProvider internal constructor(
	private val tag: String,
	private val loggerProvider: () -> AckpineLogger?
) {

	@JvmSynthetic
	internal fun withTag(tag: String): AckpineLoggerProvider {
		if (this.tag == tag) {
			return this
		}
		return AckpineLoggerProvider(tag, loggerProvider)
	}

	@JvmSynthetic
	internal fun verbose(template: String, vararg args: Any?) {
		log(Level.VERBOSE, template, null, args)
	}

	@JvmSynthetic
	internal fun verbose(throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.VERBOSE, template, throwable, args)
	}

	@JvmSynthetic
	internal fun debug(template: String, vararg args: Any?) {
		log(Level.DEBUG, template, null, args)
	}

	@JvmSynthetic
	internal fun debug(throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.DEBUG, template, throwable, args)
	}

	@JvmSynthetic
	internal fun info(template: String, vararg args: Any?) {
		log(Level.INFO, template, null, args)
	}

	@JvmSynthetic
	internal fun info(throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.INFO, template, throwable, args)
	}

	@JvmSynthetic
	internal fun warn(template: String, vararg args: Any?) {
		log(Level.WARN, template, null, args)
	}

	@JvmSynthetic
	internal fun warn(throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.WARN, template, throwable, args)
	}

	@JvmSynthetic
	internal fun error(template: String, vararg args: Any?) {
		log(Level.ERROR, template, null, args)
	}

	@JvmSynthetic
	internal fun error(throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.ERROR, template, throwable, args)
	}

	@JvmSynthetic
	@VisibleForTesting
	internal fun currentLogger(): AckpineLogger? = loggerProvider()

	private fun log(
		level: Level,
		template: String,
		throwable: Throwable?,
		args: Array<out Any?>
	) {
		val logger = loggerProvider() ?: return
		try {
			logger.log(level, tag, template, throwable, args)
		} catch (exception: Throwable) {
			Log.e("Ackpine", "Ackpine logger failed", exception)
		}
	}
}