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

package ru.solrudev.ackpine.sample.settings

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import rikka.shizuku.Shizuku

private val INSTALLER_BACKEND_KEY = stringPreferencesKey("installer_backend")
val Context.preferencesDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
	private val dataStore: DataStore<Preferences>,
	val isShizukuAvailable: Flow<Boolean> = isShizukuAvailable()
) {

	val installerBackend = combine(
		dataStore.data.map { preferences ->
			val value = preferences[INSTALLER_BACKEND_KEY] ?: return@map InstallerBackend.ROOTLESS
			InstallerBackend.valueOf(value)
		},
		isShizukuAvailable
	) { installerBackend, isShizukuAvailable ->
		if (installerBackend == InstallerBackend.SHIZUKU && !isShizukuAvailable) {
			InstallerBackend.ROOTLESS
		} else {
			installerBackend
		}
	}.distinctUntilChanged()

	suspend fun setInstallerBackend(backend: InstallerBackend) {
		dataStore.edit { preferences ->
			preferences[INSTALLER_BACKEND_KEY] = backend.name
		}
	}
}

private fun isShizukuAvailable(): Flow<Boolean> {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
		return flowOf(false)
	}
	return callbackFlow {
		send(Shizuku.pingBinder())
		val receivedListener = Shizuku.OnBinderReceivedListener {
			trySend(true)
		}
		val deadListener = Shizuku.OnBinderDeadListener {
			trySend(false)
		}
		Shizuku.addBinderReceivedListener(receivedListener)
		Shizuku.addBinderDeadListener(deadListener)
		awaitClose {
			Shizuku.removeBinderReceivedListener(receivedListener)
			Shizuku.removeBinderDeadListener(deadListener)
		}
	}
		.catch { _ -> emit(false) }
		.distinctUntilChanged()
}