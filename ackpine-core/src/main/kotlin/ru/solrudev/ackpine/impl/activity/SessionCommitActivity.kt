/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.annotation.RestrictTo
import androidx.core.view.isVisible
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.helpers.concurrent.handleResult
import ru.solrudev.ackpine.impl.installer.activity.helpers.getSerializableCompat
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt

private const val IS_SESSION_COMMITTED_KEY = "SESSION_COMMIT_ACTIVITY_IS_SESSION_COMMITTED"
private const val IS_CONFIG_CHANGE_RECREATION_KEY = "SESSION_COMMIT_ACTIVITY_IS_CONFIG_CHANGE_RECREATION"
private const val REQUEST_CODE_KEY = "SESSION_COMMIT_ACTIVITY_REQUEST_CODE"
private const val IS_LOADING_KEY = "SESSION_COMMIT_ACTIVITY_IS_LOADING"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class SessionCommitActivity<S : Session<F>, F : Failure> protected constructor(
	private val tag: String,
	private val startsActivity: Boolean,
	private val abortedStateFailureFactory: (String) -> F
) : Activity() {

	protected abstract val ackpineSessionFuture: ListenableFuture<S?>

	protected val ackpineSessionId by lazy(LazyThreadSafetyMode.NONE) {
		intent.extras?.getSerializableCompat<UUID>(SESSION_ID_KEY) ?: error("ackpineSessionId was null")
	}

	protected var requestCode = -1
		private set

	private val subscriptions = DisposableSubscriptionContainer()
	private val handler = Handler(Looper.getMainLooper())
	private val handlerCallbacks = mutableListOf<Runnable>()
	private var isSessionCommitted = false
	private var isLoading = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
		initializeState(savedInstanceState)
		setContentView(R.layout.ackpine_activity_session_commit)
		registerOnBackInvokedCallback()
		finishActivityOnTerminalSessionState()
	}

	override fun onDestroy() {
		super.onDestroy()
		subscriptions.clear()
		for (callback in handlerCallbacks) {
			handler.removeCallbacks(callback)
		}
		handlerCallbacks.clear()
	}

	@Deprecated("Deprecated in Java")
	@Suppress("DEPRECATION")
	override fun onBackPressed() {
		abortSession()
		super.onBackPressed()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(REQUEST_CODE_KEY, requestCode)
		outState.putBoolean(IS_SESSION_COMMITTED_KEY, isSessionCommitted)
		outState.putBoolean(IS_CONFIG_CHANGE_RECREATION_KEY, isChangingConfigurations)
		outState.putBoolean(IS_LOADING_KEY, isLoading)
	}

	@Suppress("UNCHECKED_CAST")
	@JvmSynthetic
	internal inline fun withCompletableSession(crossinline block: (CompletableSession<F>?) -> Unit) {
		ackpineSessionFuture.handleResult { session ->
			val completableSession = session as? CompletableSession<F>
			block(completableSession)
		}
	}

	protected fun notifySessionCommitted() {
		isSessionCommitted = true
		withCompletableSession { session ->
			session?.notifyCommitted()
		}
	}

	protected fun setLoading(isLoading: Boolean, delayMillis: Long = 0L) {
		if (isFinishing) {
			return
		}
		this.isLoading = isLoading
		if (delayMillis == 0L) {
			displayLoading(isLoading)
		} else {
			val callback = Runnable { displayLoading(isLoading) }
			handlerCallbacks += callback
			handler.postDelayed(callback, delayMillis)
		}
	}

	private fun initializeState(savedInstanceState: Bundle?) {
		if (savedInstanceState != null) {
			requestCode = savedInstanceState.getInt(REQUEST_CODE_KEY)
			isLoading = savedInstanceState.getBoolean(IS_LOADING_KEY)
			setLoading(isLoading)
			isSessionCommitted = savedInstanceState.getBoolean(IS_SESSION_COMMITTED_KEY)
			val isConfigChangeRecreation = savedInstanceState.getBoolean(IS_CONFIG_CHANGE_RECREATION_KEY)
			if (isSessionCommitted && !isConfigChangeRecreation) {
				notifySessionCommitted()
			}
		} else {
			requestCode = Random.nextInt(1000..1000000)
		}
	}

	private fun displayLoading(isLoading: Boolean) {
		findViewById<ProgressBar>(R.id.ackpine_session_commit_loading_indicator)?.isVisible = isLoading
		if (isLoading) {
			window?.setBackgroundDrawableResource(android.R.drawable.screen_background_dark_transparent)
		} else {
			window?.setBackgroundDrawable(ColorDrawable(0))
		}
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
		session?.addStateListener(subscriptions) { _, state ->
			if (state.isTerminal) {
				finishWithLaunchedActivity()
			}
		}
	}

	private fun finishWithLaunchedActivity() {
		if (startsActivity) {
			finishActivity(requestCode)
		}
		finish()
	}

	internal companion object {

		@get:JvmSynthetic
		internal const val SESSION_ID_KEY = "ACKPINE_SESSION_ID"
	}
}