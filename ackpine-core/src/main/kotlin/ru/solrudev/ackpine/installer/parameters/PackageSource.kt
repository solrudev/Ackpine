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

package ru.solrudev.ackpine.installer.parameters

public sealed class PackageSource(
	@get:JvmSynthetic
	internal val ordinal: Int
) {
	public data object Unspecified : PackageSource(0)
	public data object Store : PackageSource(1)
	public data object LocalFile : PackageSource(2)
	public data object DownloadedFile : PackageSource(3)
	public data object Other : PackageSource(4)
	
	@Suppress("unused")
	private data object NonExhaustiveWhenGuard : PackageSource(-1)

	@Suppress("RedundantVisibilityModifier")
	private companion object {

		private val values = arrayOf(Unspecified, Store, LocalFile, DownloadedFile, Other)

		@JvmField
		public val UNSPECIFIED: PackageSource = Unspecified

		@JvmField
		public val STORE: PackageSource = Store

		@JvmField
		public val LOCAL_FILE: PackageSource = LocalFile

		@JvmField
		public val DOWNLOADED_FILE: PackageSource = DownloadedFile

		@JvmField
		public val OTHER: PackageSource = Other

		@JvmSynthetic
		internal fun fromOrdinal(ordinal: Int) = values.getOrNull(ordinal) ?: Unspecified
	}
}