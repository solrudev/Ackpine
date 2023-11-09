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

package ru.solrudev.ackpine.splits

import android.os.Build
import ru.solrudev.ackpine.splits.helpers.splitTypePart

/**
 * ABI of [APK split][Apk] native code.
 */
public enum class Abi {

	ARMEABI, ARMEABI_V7A, ARM64_V8A, X86, X86_64, MIPS, MIPS64;

	public companion object {

		/**
		 * Returns an ordered list of [ABIs][Abi] supported by the device. The most preferred ABI is the first element
		 * in the list.
		 *
		 * The list is cached. Don't mutate it.
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