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

package ru.solrudev.ackpine.impl.database.dao

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.impl.HasAckpineDatabaseTest
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.installer.getApks
import ru.solrudev.ackpine.impl.installer.getV4Signatures
import ru.solrudev.ackpine.impl.testutil.createInstallSessionEntity
import ru.solrudev.ackpine.installer.parameters.InstallerType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val BASE_APK = "file:///base.apk"
private const val BASE_APK_V4_SIGNATURE = "file:///base.apk.idsig"
private const val SPLIT_1_APK = "file:///split1.apk"
private const val SPLIT_2_APK = "file:///split2.apk"
private const val SPLIT_2_APK_V4_SIGNATURE = "file:///split2.apk.idsig"

@RunWith(RobolectricTestRunner::class)
class InstallSessionDaoTest : HasAckpineDatabaseTest() {

	@Test
	fun installUrisWithoutV4SignaturesRoundTrip() {
		val id = UUID.randomUUID().toString()
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = id,
				state = SessionEntity.State.PENDING,
				installerType = InstallerType.SESSION_BASED,
				uris = listOf(BASE_APK)
			)
		)

		val restored = assertNotNull(database.installSessionDao().getInstallSession(id))

		assertEquals(listOf(BASE_APK), restored.uris.map { it.uri })
		assertEquals(listOf(null), restored.uris.map { it.v4SignatureUri })
		assertEquals(emptyMap(), restored.getV4Signatures())
	}

	@Test
	fun v4SignatureUrisRoundTripPairedWithTheirApks() {
		val id = UUID.randomUUID().toString()
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = id,
				state = SessionEntity.State.PENDING,
				installerType = InstallerType.SESSION_BASED,
				uris = listOf(BASE_APK, SPLIT_1_APK, SPLIT_2_APK),
				v4SignatureUris = mapOf(
					BASE_APK to BASE_APK_V4_SIGNATURE,
					SPLIT_2_APK to SPLIT_2_APK_V4_SIGNATURE
				)
			)
		)

		val restored = assertNotNull(database.installSessionDao().getInstallSession(id))

		// The pairing is row-local, so it survives regardless of the order rows come back in.
		assertEquals(
			mapOf(
				BASE_APK to BASE_APK_V4_SIGNATURE,
				SPLIT_1_APK to null,
				SPLIT_2_APK to SPLIT_2_APK_V4_SIGNATURE
			),
			restored.uris.associate { it.uri to it.v4SignatureUri }
		)
		assertEquals(3, restored.getApks().size)
		assertEquals(2, restored.getV4Signatures().size)
	}
}
