/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.splits.parsing

import com.android.apksig.internal.apk.AndroidBinXmlParser
import java.nio.ByteBuffer

@JvmSynthetic
internal const val ANDROID_MANIFEST_FILE_NAME = "AndroidManifest.xml"

@JvmSynthetic
internal fun AndroidManifest(androidManifest: ByteBuffer): AndroidManifest? {
	var seenManifestElement = false
	val manifest = mutableMapOf<String, String>()
	val parser = AndroidBinXmlParser(androidManifest)
	while (parser.next() != AndroidBinXmlParser.EVENT_END_DOCUMENT) {
		if (parser.eventType != AndroidBinXmlParser.EVENT_START_ELEMENT) {
			continue
		}
		if (parser.name != "manifest" || parser.namespace.isNotEmpty() || parser.depth != 1) {
			continue
		}
		if (seenManifestElement) {
			return null
		}
		seenManifestElement = true
		for (index in 0..<parser.attributeCount) {
			if (parser.getAttributeName(index).isEmpty()) {
				continue
			}
			val namespace = parser.getAttributeNamespace(index)
				.takeIf { it.isNotEmpty() }
				?.plus(':')
				.orEmpty()
			val attribute = parser.getAttributeName(index)
			manifest["$namespace$attribute"] = parser.getAttributeStringValue(index)
		}
	}
	if (!seenManifestElement) {
		return null
	}
	return AndroidManifest(manifest)
}