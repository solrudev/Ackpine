package ru.solrudev.ackpine.sample.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

fun ContentResolver.getDisplayName(uri: Uri): String {
	query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
		if (cursor == null || !cursor.moveToFirst()) {
			return ""
		}
		return cursor.getString(0)
	}
}