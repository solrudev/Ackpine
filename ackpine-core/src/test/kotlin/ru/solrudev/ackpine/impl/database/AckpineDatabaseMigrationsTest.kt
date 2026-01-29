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

package ru.solrudev.ackpine.impl.database

import android.os.Build
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TEST_DB_NAME = "migration_tests.db"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class AckpineDatabaseMigrationsTest {

	@get:Rule
	val migrationTestHelper = MigrationTestHelper(
		InstrumentationRegistry.getInstrumentation(),
		AckpineDatabase::class.java
	)

	@Test
	fun migration4To5RecreatesSessionsTableWithTypeColumn() {
		migrationTestHelper.createDatabase(TEST_DB_NAME, 4).use { db ->
			db.execSQL(
				"""
				INSERT INTO sessions (id, state, confirmation, notification_title, notification_text,
				                       notification_icon, require_user_action, last_launch_timestamp)
				VALUES ('test-session-1', 'PENDING', 'DEFERRED', X'00', X'00', 0, 1, 0)
				""".trimIndent()
			)
		}

		migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 5, true, Migration_4_5).use { db ->
			db.query("SELECT * FROM sessions").use { cursor ->
				assertEquals(0, cursor.count, "Sessions table should be empty after migration")
			}

			db.execSQL(
				"""
				INSERT INTO sessions (id, type, state, confirmation, notification_title, notification_text,
				                       notification_icon, require_user_action, last_launch_timestamp)
				VALUES ('new-session', 'INSTALL', 'PENDING', 'DEFERRED', X'00', X'00', 0, 1, 0)
				""".trimIndent()
			)

			db.query("SELECT type FROM sessions WHERE id = 'new-session'").use { cursor ->
				cursor.moveToFirst()
				assertEquals("INSTALL", cursor.getString(0))
			}
		}
	}

	@Test
	fun migration4To5ClearsRelatedTables() {
		migrationTestHelper.createDatabase(TEST_DB_NAME, 4).use { db ->
			db.execSQL(
				"""
				INSERT INTO sessions (id, state, confirmation, notification_title, notification_text,
				                       notification_icon, require_user_action, last_launch_timestamp)
				VALUES ('test-session', 'PENDING', 'DEFERRED', X'00', X'00', 0, 1, 0)
				""".trimIndent()
			)
			db.execSQL(
				"""INSERT INTO sessions_installer_types (session_id, installer_type)
				          VALUES ('test-session', 'DEFAULT')""".trimIndent()
			)
			db.execSQL("INSERT INTO sessions_progress (session_id, progress, max) VALUES ('test-session', 50, 100)")
			db.execSQL(
				"""INSERT INTO sessions_notification_ids (session_id, notification_id)
				          VALUES ('test-session', 123)""".trimIndent()
			)
		}

		migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 5, true, Migration_4_5).use { db ->
			db.query("SELECT COUNT(*) FROM sessions_installer_types").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0))
			}
			db.query("SELECT COUNT(*) FROM sessions_progress").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0))
			}
			db.query("SELECT COUNT(*) FROM sessions_notification_ids").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0))
			}
		}
	}

	@Test
	fun migration7To8AddsLastCommitTimestampColumn() {
		migrationTestHelper.createDatabase(TEST_DB_NAME, 7).use { db ->
			db.execSQL(
				"""
				INSERT INTO sessions (id, type, state, confirmation, notification_title, notification_text,
				                       notification_icon, require_user_action, last_launch_timestamp)
				VALUES ('test-session', 'INSTALL', 'PENDING', 'DEFERRED', X'00', X'00', X'00', 1, 0)
				""".trimIndent()
			)
		}

		migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 8, true, Migration_7_8).use { db ->
			db.query("SELECT COUNT(*) FROM sessions").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0))
			}

			db.execSQL(
				"""
				INSERT INTO sessions (id, type, state, confirmation, notification_title, notification_text,
				                       notification_icon, require_user_action, last_launch_timestamp,
									   last_commit_timestamp)
				VALUES ('new-session', 'INSTALL', 'PENDING', 'DEFERRED', X'00', X'00', X'00', 1, 100, 200)
				""".trimIndent()
			)

			db.query("SELECT last_commit_timestamp FROM sessions WHERE id = 'new-session'").use { cursor ->
				cursor.moveToFirst()
				assertEquals(200, cursor.getLong(0))
			}
		}
	}

	@Test
	fun migration7To8ClearsAllTables() {
		migrationTestHelper.createDatabase(TEST_DB_NAME, 7).use { db ->
			db.execSQL(
				"""
				INSERT INTO sessions (id, type, state, confirmation, notification_title, notification_text,
				                       notification_icon, require_user_action, last_launch_timestamp)
				VALUES ('test-session', 'INSTALL', 'ACTIVE', 'DEFERRED', X'00', X'00', X'00', 1, 0)
				""".trimIndent()
			)
			db.execSQL(
				"""INSERT INTO sessions_installer_types (session_id, installer_type)
				          VALUES ('test-session', 'SESSION_BASED')""".trimIndent()
			)
			db.execSQL(
				"""INSERT INTO sessions_install_modes (session_id, install_mode)
				          VALUES ('test-session', 'FULL')""".trimIndent()
			)
		}

		migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 8, true, Migration_7_8).use { db ->
			db.query("SELECT COUNT(*) FROM sessions").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0))
			}
			db.query("SELECT COUNT(*) FROM sessions_installer_types").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0))
			}
			db.query("SELECT COUNT(*) FROM sessions_install_modes").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0))
			}
		}
	}

	@Test
	fun migration12To13RecreatesPluginsTableWithoutParametersColumn() {
		migrationTestHelper.createDatabase(TEST_DB_NAME, 12).use { db ->
			db.execSQL(
				"""
				INSERT INTO sessions (id, type, state, confirmation, notification_title, notification_text,
				                       notification_icon, require_user_action, last_launch_timestamp,
									   last_commit_timestamp)
				VALUES ('session-without-plugin', 'INSTALL', 'PENDING', 'DEFERRED', X'00', X'00', X'00', 1, 0, 0)
				""".trimIndent()
			)
			db.execSQL(
				"""
				INSERT INTO sessions (id, type, state, confirmation, notification_title, notification_text,
				                       notification_icon, require_user_action, last_launch_timestamp,
									   last_commit_timestamp)
				VALUES ('session-with-plugin', 'INSTALL', 'ACTIVE', 'DEFERRED', X'00', X'00', X'00', 1, 0, 0)
				""".trimIndent()
			)
			db.execSQL(
				"""INSERT INTO sessions_plugins (session_id, plugin_class_name, plugin_parameters)
				          VALUES ('session-with-plugin', 'com.example.Plugin', X'00')""".trimIndent()
			)
		}

		migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 13, true, Migration_12_13).use { db ->
			db.query("SELECT COUNT(*) FROM sessions WHERE id = 'session-with-plugin'").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0), "Session with plugin should be deleted")
			}

			db.query("SELECT COUNT(*) FROM sessions WHERE id = 'session-without-plugin'").use { cursor ->
				cursor.moveToFirst()
				assertEquals(1, cursor.getInt(0), "Session without plugin should remain")
			}

			db.query("SELECT COUNT(*) FROM sessions_plugins").use { cursor ->
				cursor.moveToFirst()
				assertEquals(0, cursor.getInt(0))
			}
		}
	}

	@Test
	fun migration12To13DeletesSessionsWithPlugins() {
		migrationTestHelper.createDatabase(TEST_DB_NAME, 12).use { db ->
			for (i in 1..3) {
				db.execSQL(
					"""
					INSERT INTO sessions (id, type, state, confirmation, notification_title, notification_text,
					                       notification_icon, require_user_action, last_launch_timestamp,
										   last_commit_timestamp)
					VALUES ('session-$i', 'INSTALL', 'PENDING', 'DEFERRED', X'00', X'00', X'00', 1, 0, 0)
					""".trimIndent()
				)
			}
			db.execSQL(
				"""INSERT INTO sessions_plugins (session_id, plugin_class_name, plugin_parameters)
				          VALUES ('session-1', 'Plugin1', X'00')""".trimIndent()
			)
			db.execSQL(
				"""INSERT INTO sessions_plugins (session_id, plugin_class_name, plugin_parameters)
				          VALUES ('session-3', 'Plugin2', X'00')""".trimIndent()
			)
		}

		migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 13, true, Migration_12_13).use { db ->
			db.query("SELECT id FROM sessions ORDER BY id").use { cursor ->
				assertEquals(1, cursor.count)
				cursor.moveToFirst()
				assertEquals("session-2", cursor.getString(0))
			}
		}
	}
}