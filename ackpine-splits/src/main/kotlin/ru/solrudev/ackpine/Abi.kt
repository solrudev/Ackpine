package ru.solrudev.ackpine

import android.os.Build
import ru.solrudev.ackpine.helpers.splitTypePart

public enum class Abi {

	ARMEABI, ARMEABI_V7A, ARM64_V8A, X86, X86_64, MIPS, MIPS64;

	public companion object {

		private val abis = Abi.values().map { it.name.lowercase() }.toSet()

		@JvmStatic
		public val deviceAbis: List<Abi> by lazy(LazyThreadSafetyMode.NONE) {
			Build.SUPPORTED_ABIS.map { valueOf(it.replace(oldChar = '-', newChar = '_').uppercase()) }
		}

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