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

@file:Suppress("ClassName")

package ru.solrudev.ackpine.impl.database

import android.os.Build
import androidx.annotation.RestrictTo
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object Migration_4_5 : Migration(4, 5) {
	override fun migrate(db: SupportSQLiteDatabase) = db.migrate {
		execSQL("DROP TABLE sessions")
		execSQL("CREATE TABLE IF NOT EXISTS sessions (id TEXT NOT NULL, type TEXT NOT NULL, state TEXT NOT NULL, confirmation TEXT NOT NULL, notification_title BLOB NOT NULL, notification_text BLOB NOT NULL, notification_icon INTEGER NOT NULL, require_user_action INTEGER NOT NULL DEFAULT true, last_launch_timestamp INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
		execSQL("CREATE INDEX IF NOT EXISTS index_sessions_type ON sessions (type)")
		execSQL("CREATE INDEX IF NOT EXISTS index_sessions_state ON sessions (state)")
		execSQL("CREATE INDEX IF NOT EXISTS index_sessions_last_launch_timestamp ON sessions (last_launch_timestamp)")
		execSQL("DELETE FROM sessions_installer_types")
		execSQL("DELETE FROM sessions_install_failures")
		execSQL("DELETE FROM sessions_uninstall_failures")
		execSQL("DELETE FROM sessions_install_uris")
		execSQL("DELETE FROM sessions_package_names")
		execSQL("DELETE FROM sessions_progress")
		execSQL("DELETE FROM sessions_native_session_ids")
		execSQL("DELETE FROM sessions_notification_ids")
		execSQL("DELETE FROM sessions_names")
	}
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object Migration_7_8 : Migration(7, 8) {
	override fun migrate(db: SupportSQLiteDatabase) = db.migrate {
		execSQL("DROP TABLE sessions")
		execSQL("CREATE TABLE IF NOT EXISTS sessions (id TEXT NOT NULL, type TEXT NOT NULL, state TEXT NOT NULL, confirmation TEXT NOT NULL, notification_title BLOB NOT NULL, notification_text BLOB NOT NULL, notification_icon BLOB NOT NULL, require_user_action INTEGER NOT NULL DEFAULT true, last_launch_timestamp INTEGER NOT NULL DEFAULT 0, last_commit_timestamp INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
		execSQL("CREATE INDEX IF NOT EXISTS index_sessions_type ON sessions (type)")
		execSQL("CREATE INDEX IF NOT EXISTS index_sessions_state ON sessions (state)")
		execSQL("CREATE INDEX IF NOT EXISTS index_sessions_last_launch_timestamp ON sessions (last_launch_timestamp)")
		execSQL("CREATE INDEX IF NOT EXISTS index_sessions_last_commit_timestamp ON sessions (last_commit_timestamp)")
		execSQL("DELETE FROM sessions_installer_types")
		execSQL("DELETE FROM sessions_install_failures")
		execSQL("DELETE FROM sessions_uninstall_failures")
		execSQL("DELETE FROM sessions_install_uris")
		execSQL("DELETE FROM sessions_package_names")
		execSQL("DELETE FROM sessions_progress")
		execSQL("DELETE FROM sessions_native_session_ids")
		execSQL("DELETE FROM sessions_notification_ids")
		execSQL("DELETE FROM sessions_names")
		execSQL("DELETE FROM sessions_install_modes")
		execSQL("DELETE FROM sessions_last_install_timestamps")
	}
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object Migration_12_13 : Migration(12, 13) {
	override fun migrate(db: SupportSQLiteDatabase) = db.migrate {
		execSQL("DELETE FROM sessions WHERE EXISTS (SELECT 1 FROM sessions_plugins WHERE sessions_plugins.session_id = sessions.id)")
		execSQL("DROP TABLE sessions_plugins")
		execSQL("CREATE TABLE IF NOT EXISTS sessions_plugins (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, session_id TEXT NOT NULL, plugin_class_name TEXT NOT NULL, FOREIGN KEY(session_id) REFERENCES sessions(id) ON UPDATE CASCADE ON DELETE CASCADE )")
		execSQL("CREATE INDEX IF NOT EXISTS index_sessions_plugins_session_id ON sessions_plugins (session_id)")
	}
}

private inline fun SupportSQLiteDatabase.migrate(actions: SupportSQLiteDatabase.() -> Unit) {
	val supportsDeferForeignKeys = Build.VERSION.SDK_INT >= 21
	try {
		if (!supportsDeferForeignKeys) {
			execSQL("PRAGMA foreign_keys = FALSE")
		}
		beginTransaction()
		if (supportsDeferForeignKeys) {
			execSQL("PRAGMA defer_foreign_keys = TRUE")
		}
		actions()
		setTransactionSuccessful()
	} finally {
		endTransaction()
		if (!supportsDeferForeignKeys) {
			execSQL("PRAGMA foreign_keys = TRUE")
		}
		query("PRAGMA wal_checkpoint(FULL)").close()
		if (!inTransaction()) {
			execSQL("VACUUM")
		}
	}
}