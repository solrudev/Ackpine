package ru.solrudev.ackpine.splits

import android.os.Build
import ru.solrudev.ackpine.helpers.splitTypePart

/**
 * ABI of [APK split][Apk] native code.
 */
public enum class Abi {

	ARMEABI, ARMEABI_V7A, ARM64_V8A, X86, X86_64, MIPS, MIPS64;

	public companion object {

		/**
		 * Returns an ordered list of [ABIs][Abi] supported by the device. The most preferred ABI is the first element
		 * in the list.
		 */
		@JvmStatic
		public val deviceAbis: List<Abi> by lazy(LazyThreadSafetyMode.NONE) {
			Build.SUPPORTED_ABIS.map { valueOf(it.replace(oldChar = '-', newChar = '_').uppercase()) }
		}

		private val abis = Abi.values().map { it.name.lowercase() }.toSet()

		@JvmSynthetic
		internal fun fromSplitName(name: String): Abi? {
			val abiPart = splitTypePart(name) ?: return null
			if (abiPart in abis) {
				return Abi.valueOf(abiPart.uppercase())
			}
			return null
		}
	}
}