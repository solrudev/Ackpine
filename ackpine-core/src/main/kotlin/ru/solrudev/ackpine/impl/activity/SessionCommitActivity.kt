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
import android.content.Intent
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
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import kotlin.random.Random
import kotlin.random.nextInt

private const val IS_CONFIG_CHANGE_RECREATION_KEY = "SESSION_COMMIT_ACTIVITY_IS_CONFIG_CHANGE_RECREATION"
private const val REQUEST_CODE_KEY = "SESSION_COMMIT_ACTIVITY_REQUEST_CODE"
private const val IS_LOADING_KEY = "SESSION_COMMIT_ACTIVITY_IS_LOADING"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class SessionCommitActivity<F : Failure> protected constructor(
	private val tag: String,
	private val abortedStateFailureFactory: (String) -> F
) : Activity() {

	protected abstract val ackpineSessionFuture: ListenableFuture<out CompletableSession<F>?>

	protected val ackpineSessionId by lazy(LazyThreadSafetyMode.NONE) {
		SessionIdIntents.getSessionId(intent)
	}

	private val subscriptions = DisposableSubscriptionContainer()
	private val handler = Handler(Looper.getMainLooper())
	private val handlerCallbacks = mutableListOf<Runnable>()
	private var requestCode = -1
	private var isLoading = false
	private var isOnActivityResultCalled = false

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
		if (!isOnActivityResultCalled) {
			abortSession()
		}
		super.onBackPressed()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(REQUEST_CODE_KEY, requestCode)
		outState.putBoolean(IS_CONFIG_CHANGE_RECREATION_KEY, isChangingConfigurations)
		outState.putBoolean(IS_LOADING_KEY, isLoading)
	}

	final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		isOnActivityResultCalled = true
		if (requestCode != this.requestCode) {
			return
		}
		onActivityResult(resultCode)
	}

	protected open fun onActivityResult(resultCode: Int) { // no-op by default
	}

	protected fun startActivityForResult(intent: Intent) = startActivityForResult(intent, requestCode)

	protected fun completeSession(state: Session.State.Completed<F>) = withCompletableSession { session ->
		session?.complete(state)
	}

	protected fun completeSessionExceptionally(exception: Exception) = withCompletableSession { session ->
		session?.completeExceptionally(exception)
	}

	protected open fun shouldNotifyWhenCommitted(): Boolean = true

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

	protected fun abortSession(message: String? = null) = withCompletableSession { session ->
		session?.complete(
			Session.State.Failed(
				abortedStateFailureFactory(message ?: "$tag was finished by user")
			)
		)
	}

	protected fun withCompletableSession(block: (CompletableSession<F>?) -> Unit) {
		ackpineSessionFuture.handleResult(block)
	}

	private fun notifySessionCommitted() {
		if (!shouldNotifyWhenCommitted()) {
			return
		}
		withCompletableSession { session ->
			session?.notifyCommitted()
		}
	}

	private fun initializeState(savedInstanceState: Bundle?) {
		if (savedInstanceState != null) {
			requestCode = savedInstanceState.getInt(REQUEST_CODE_KEY)
			isLoading = savedInstanceState.getBoolean(IS_LOADING_KEY)
			setLoading(isLoading)
			val isConfigChangeRecreation = savedInstanceState.getBoolean(IS_CONFIG_CHANGE_RECREATION_KEY)
			if (!isConfigChangeRecreation) {
				notifySessionCommitted()
			}
		} else {
			notifySessionCommitted()
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
				if (!isOnActivityResultCalled) {
					abortSession()
				}
			}
		}
	}

	private fun finishActivityOnTerminalSessionState() = ackpineSessionFuture.handleResult { session ->
		session?.addStateListener(subscriptions) { _, state ->
			if (state.isTerminal) {
				finishActivity(requestCode)
				finish()
			}
		}
	}
}