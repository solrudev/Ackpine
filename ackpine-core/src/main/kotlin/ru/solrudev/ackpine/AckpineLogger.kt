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

package ru.solrudev.ackpine

import android.util.Log
import java.util.Locale

/**
 * Logger for Ackpine runtime events.
 *
 * Ackpine message templates are [String.format]-compatible. [Logcat] renders them with `Locale.ROOT`.
 */
public fun interface AckpineLogger {

	/**
	 * Logs an Ackpine event.
	 *
	 * [template] is [String.format]-compatible and must be interpreted together with [args].
	 */
	public fun log(
		level: Level,
		tag: String,
		template: String,
		throwable: Throwable?,
		args: Array<out Any?>
	)

	/**
	 * Logs a [Level.VERBOSE] event.
	 */
	public fun verbose(tag: String, template: String, vararg args: Any?) {
		log(Level.VERBOSE, tag, template, null, args)
	}

	/**
	 * Logs a [Level.VERBOSE] event with an associated [throwable].
	 */
	public fun verbose(tag: String, throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.VERBOSE, tag, template, throwable, args)
	}

	/**
	 * Logs a [Level.DEBUG] event.
	 */
	public fun debug(tag: String, template: String, vararg args: Any?) {
		log(Level.DEBUG, tag, template, null, args)
	}

	/**
	 * Logs a [Level.DEBUG] event with an associated [throwable].
	 */
	public fun debug(tag: String, throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.DEBUG, tag, template, throwable, args)
	}

	/**
	 * Logs a [Level.INFO] event.
	 */
	public fun info(tag: String, template: String, vararg args: Any?) {
		log(Level.INFO, tag, template, null, args)
	}

	/**
	 * Logs a [Level.INFO] event with an associated [throwable].
	 */
	public fun info(tag: String, throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.INFO, tag, template, throwable, args)
	}

	/**
	 * Logs a [Level.WARN] event.
	 */
	public fun warn(tag: String, template: String, vararg args: Any?) {
		log(Level.WARN, tag, template, null, args)
	}

	/**
	 * Logs a [Level.WARN] event with an associated [throwable].
	 */
	public fun warn(tag: String, throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.WARN, tag, template, throwable, args)
	}

	/**
	 * Logs a [Level.ERROR] event.
	 */
	public fun error(tag: String, template: String, vararg args: Any?) {
		log(Level.ERROR, tag, template, null, args)
	}

	/**
	 * Logs a [Level.ERROR] event with an associated [throwable].
	 */
	public fun error(tag: String, throwable: Throwable, template: String, vararg args: Any?) {
		log(Level.ERROR, tag, template, throwable, args)
	}

	/**
	 * Severity level for an Ackpine log event.
	 *
	 * * [VERBOSE] &mdash; fine-grained diagnostic detail.
	 * * [DEBUG] &mdash; diagnostic messages useful for troubleshooting.
	 * * [INFO] &mdash; messages about normal operation and notable events.
	 * * [WARN] &mdash; unexpected or recoverable conditions.
	 * * [ERROR] &mdash; failures or invalid conditions.
	 */
	public enum class Level {

		/**
		 * Fine-grained diagnostic detail.
		 */
		VERBOSE,

		/**
		 * Diagnostic messages useful for troubleshooting.
		 */
		DEBUG,

		/**
		 * Messages about normal operation and notable events.
		 */
		INFO,

		/**
		 * Unexpected or recoverable conditions.
		 */
		WARN,

		/**
		 * Failures or invalid conditions.
		 */
		ERROR
	}

	/**
	 * Built-in logger that forwards Ackpine events to Android logcat.
	 *
	 * Templates are rendered with `Locale.ROOT`.
	 */
	public class Logcat : AckpineLogger {

		override fun log(
			level: Level,
			tag: String,
			template: String,
			throwable: Throwable?,
			args: Array<out Any?>
		) {
			val message = format(template, args)
			when (level) {
				Level.VERBOSE -> Log.v(tag, message, throwable)
				Level.DEBUG -> Log.d(tag, message, throwable)
				Level.INFO -> Log.i(tag, message, throwable)
				Level.WARN -> Log.w(tag, message, throwable)
				Level.ERROR -> Log.e(tag, message, throwable)
			}
		}

		private fun format(template: String, args: Array<out Any?>): String {
			if (args.isEmpty()) {
				return template
			}
			return try {
				String.format(Locale.ROOT, template, *args)
			} catch (exception: RuntimeException) {
				"$template [formatting failed: ${exception.message}; args=${args.contentDeepToString()}]"
			}
		}
	}
}