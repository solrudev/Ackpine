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

package ru.solrudev.ackpine.impl.activity

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RestrictTo
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.helpers.handleResult
import ru.solrudev.ackpine.impl.activity.helpers.clearTurnScreenOnSettings
import ru.solrudev.ackpine.impl.activity.helpers.turnScreenOnWhenLocked
import ru.solrudev.ackpine.impl.installer.activity.helpers.getSerializableCompat
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class SessionCommitActivity<S : Session<F>, F : Failure>(
	private val tag: String,
	private val requestCode: Int = -1,
	private val abortedStateFailureFactory: (String) -> F
) : Activity() {

	protected abstract val ackpineSessionFuture: ListenableFuture<S?>

	protected val ackpineSessionId by lazy(LazyThreadSafetyMode.NONE) {
		intent.extras?.getSerializableCompat<UUID>(SESSION_ID_KEY) ?: error("ackpineSessionId was null")
	}

	private val subscriptions = DisposableSubscriptionContainer()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.ackpine_activity_session_commit)
		turnScreenOnWhenLocked()
		registerOnBackInvokedCallback()
		finishActivityOnTerminalSessionState()
	}

	override fun onDestroy() {
		super.onDestroy()
		subscriptions.clear()
		clearTurnScreenOnSettings()
	}

	@Deprecated("Deprecated in Java")
	@Suppress("DEPRECATION")
	override fun onBackPressed() {
		abortSession()
		super.onBackPressed()
	}

	private fun registerOnBackInvokedCallback() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			onBackInvokedDispatcher.registerOnBackInvokedCallback(1000) {
				abortSession()
			}
		}
	}

	private fun abortSession() = withCompletableSession { session ->
		session?.complete(
			Session.State.Failed(
				abortedStateFailureFactory("$tag was finished by user")
			)
		)
	}

	private fun finishActivityOnTerminalSessionState() = ackpineSessionFuture.handleResult { session ->
		val subscription = session?.addStateListener { _, state ->
			if (state.isTerminal) {
				finishWithLaunchedActivity()
			}
		}
		subscription?.let(subscriptions::add)
	}

	private fun finishWithLaunchedActivity() {
		if (requestCode != -1) {
			finishActivity(requestCode)
		}
		finish()
	}

	@Suppress("UNCHECKED_CAST")
	@JvmSynthetic
	internal inline fun withCompletableSession(crossinline block: (CompletableSession<F>?) -> Unit) {
		ackpineSessionFuture.handleResult { session ->
			val completableSession = session as? CompletableSession<F>
			block(completableSession)
		}
	}

	internal companion object {

		@get:JvmSynthetic
		internal const val SESSION_ID_KEY = "ACKPINE_SESSION_ID"
	}
}