package ru.solrudev.ackpine.splits.parsing

import com.android.apksig.internal.apk.AndroidBinXmlParser
import ru.solrudev.ackpine.helpers.entries
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.ZipInputStream

private const val ANDROID_MANIFEST_FILE_NAME = "AndroidManifest.xml"

@JvmSynthetic
internal fun parseAndroidManifest(androidManifest: ByteBuffer): AndroidManifest? {
	var seenManifestElement = false
	val manifest = mutableMapOf<String, String>()
	val parser = AndroidBinXmlParser(androidManifest)
	while (parser.next() != AndroidBinXmlParser.EVENT_END_DOCUMENT) {
		if (parser.eventType != AndroidBinXmlParser.EVENT_START_ELEMENT) {
			continue
		}
		if (parser.name == "manifest" && parser.namespace.isEmpty() && parser.depth == 1) {
			if (seenManifestElement) {
				return null
			}
			seenManifestElement = true
			for (index in 0 until parser.attributeCount) {
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
	}
	if (!seenManifestElement) {
		return null
	}
	return AndroidManifest(manifest)
}

@JvmSynthetic
internal fun ZipInputStream.androidManifest(): ByteBuffer? {
	val buffer = ByteArrayOutputStream()
	entries().firstOrNull { it.name == ANDROID_MANIFEST_FILE_NAME } ?: return null
	copyTo(buffer)
	return ByteBuffer.wrap(buffer.toByteArray())
}