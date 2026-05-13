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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LOGS_DIR_NAME = "logs"

fun interface LogcatExporter {
	suspend fun export(): Uri
}

class DefaultLogcatExporter(private val context: Context) : LogcatExporter {

	override suspend fun export(): Uri = runInterruptible(Dispatchers.IO) {
		val logsDir = File(context.cacheDir, LOGS_DIR_NAME).apply { mkdirs() }
		val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
		val file = File(logsDir, "ackpine-logcat-$timestamp.txt")
		dumpLogcatForOwnPid(file)
		FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
	}

	private fun dumpLogcatForOwnPid(destination: File) {
		val pid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			"--pid=${Process.myPid()}"
		} else {
			""
		}
		val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", pid)
			.redirectErrorStream(true)
			.start()
		try {
			destination.bufferedWriter().use { writer ->
				writer.write("Ackpine Log\n")
				writer.write("Version: ${context.versionName}\n")
				writer.write("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
				writer.write("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
				writer.newLine()
				process.inputStream.bufferedReader().useLines { lines ->
					for (line in lines) {
						writer.write(line)
						writer.newLine()
					}
				}
			}
			val status = process.waitFor()
			if (status != 0) {
				throw RuntimeException("logcat process exited with status $status")
			}
		} finally {
			process.destroy()
		}
	}

	private val Context.versionName: String
		get() {
			val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
			} else {
				packageManager.getPackageInfo(packageName, 0)
			}
			return packageInfo.versionName ?: "unknown"
		}
}