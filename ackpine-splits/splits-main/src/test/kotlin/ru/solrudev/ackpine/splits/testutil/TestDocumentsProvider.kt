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

package ru.solrudev.ackpine.splits.testutil

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ProviderInfo
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import java.io.File

class TestDocumentsProvider : ContentProvider() {

	override fun onCreate() = true

	override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
		val file = File.createTempFile("doc-", null)
		file.deleteOnExit()
		val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
		Shadow.extract<ShadowBlockingParcelFileDescriptor>(pfd).isInvalid = true
		return pfd
	}

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		selection: String?,
		selectionArgs: Array<out String>?,
		sortOrder: String?
	) = null

	override fun getType(uri: Uri) = null
	override fun insert(uri: Uri, values: ContentValues?) = null
	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
	override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

	companion object {

		const val AUTHORITY = "com.android.externalstorage.documents"

		fun setup() {
			Robolectric.buildContentProvider(TestDocumentsProvider::class.java)
				.create(ProviderInfo().apply { authority = AUTHORITY })
			val context = ApplicationProvider.getApplicationContext<Context>()
			val shadowPm = shadowOf(context.packageManager)
			val componentName = ComponentName(context.packageName, TestDocumentsProvider::class.java.name)
			shadowPm.addOrUpdateProvider(
				ProviderInfo().apply {
					authority = AUTHORITY
					packageName = context.packageName
					name = componentName.className
				}
			)
			shadowPm.addIntentFilterForProvider(
				componentName,
				IntentFilter(DocumentsContract.PROVIDER_INTERFACE)
			)
		}
	}
}