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

package ru.solrudev.ackpine.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class HasAckpineDatabaseTest {

	protected val context: Context = ApplicationProvider.getApplicationContext()

	internal lateinit var database: AckpineDatabase
		private set

	@BeforeTest
	fun setUp() {
		database = Room
			.inMemoryDatabaseBuilder(context, AckpineDatabase::class.java)
			.allowMainThreadQueries()
			.build()
	}

	@AfterTest
	fun tearDown() = database.close()
}