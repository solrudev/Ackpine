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

package ru.solrudev.ackpine.remote

import android.os.RemoteException
import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.plusAssign
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A [Session] bridge for IPC.
 */
public class RemoteSession internal constructor(private val session: ISession) {

	/**
	 * @see [Session.id]
	 */
	public val id: UUID
		get() = UUID.fromString(session.id)

	private val stateListeners = ConcurrentHashMap<StateListener, ISessionStateListener>()

	/**
	 * @see [Session.launch]
	 */
	public fun launch(): Boolean = session.launch()

	/**
	 * @see [Session.commit]
	 */
	public fun commit(): Boolean = session.commit()

	/**
	 * @see [Session.cancel]
	 */
	public fun cancel(): Unit = session.cancel()

	/**
	 * @see [Session.addStateListener]
	 */
	public fun addStateListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: StateListener
	): DisposableSubscription {
		val stateListener = object : ISessionStateListener.Stub() {
			override fun onStateChanged(id: String, state: String) {
				listener.onStateChanged(UUID.fromString(id), State.valueOf(state))
			}

			override fun onFailed(id: String, failure: String) {
				listener.onStateChanged(UUID.fromString(id), State.Failed(RemoteFailure.valueOf(failure)))
			}
		}
		val currentListener = stateListeners.putIfAbsent(listener, stateListener)
		if (currentListener != null) {
			return DummyDisposableSubscription
		}
		session.addStateListener(stateListener)
		val subscription = StateDisposableSubscription(this, listener)
		subscriptionContainer += subscription
		return subscription
	}

	/**
	 * @see [Session.removeStateListener]
	 */
	public fun removeStateListener(listener: StateListener) {
		val stateListener = stateListeners.remove(listener) ?: return
		session.removeStateListener(stateListener)
	}

	/**
	 * Represents a state of [RemoteSession].
	 */
	public sealed class State(
		internal val name: String,

		/**
		 * Returns whether the session is in terminal state.
		 *
		 * [Cancelled], [Succeeded] and [Failed] are considered as terminal states.
		 */
		public val isTerminal: Boolean = false
	) {

		public data object Pending : State("Pending")
		public data object Active : State("Active")
		public data object Awaiting : State("Awaiting")
		public data object Committed : State("Committed")
		public data object Cancelled : State("Cancelled", isTerminal = true)
		public data object Succeeded : State("Succeeded", isTerminal = true)
		public data class Failed(val failure: RemoteFailure) : State("Failed", isTerminal = true)

		internal companion object {
			fun valueOf(value: String, failure: String? = null): State {
				return when (value) {
					Pending.name -> Pending
					Active.name -> Active
					Awaiting.name -> Awaiting
					Committed.name -> Committed
					Cancelled.name -> Cancelled
					Succeeded.name -> Succeeded
					"Failed" -> Failed(RemoteFailure.valueOf(requireNotNull(failure)))
					else -> throw NoWhenBranchMatchedException()
				}
			}
		}
	}

	/**
	 * Callback interface for listening to [RemoteSession] state updates.
	 */
	public fun interface StateListener {

		/**
		 * Notifies about state update.
		 * @param sessionId ID of the session which had its state updated.
		 * @param state session's state.
		 */
		public fun onStateChanged(sessionId: UUID, state: State)
	}

	private class StateDisposableSubscription(
		session: RemoteSession,
		listener: StateListener
	) : DisposableSubscription {

		private val session = WeakReference(session)
		private val listener = WeakReference(listener)

		override var isDisposed: Boolean = false
			private set

		override fun dispose() {
			if (isDisposed) {
				return
			}
			val listener = this.listener.get()
			if (listener != null) {
				try {
					session.get()?.removeStateListener(listener)
				} catch (_: RemoteException) { // ignore
				}
			}
			this.listener.clear()
			session.clear()
			isDisposed = true
		}
	}
}

internal class RemoteSessionImpl(private val session: Session<*>) : ISession.Stub() {

	private val stateListeners = ConcurrentHashMap<ISessionStateListener, Session.StateListener<Failure>>()

	override fun getId() = session.id.toString()
	override fun launch() = session.launch()
	override fun commit() = session.commit()
	override fun cancel() = session.cancel()

	override fun addStateListener(listener: ISessionStateListener) {
		val stateListener = Session.StateListener { sessionId, state ->
			when (state) {
				Session.State.Active -> listener.onStateChanged(
					sessionId.toString(), RemoteSession.State.Active.name
				)

				Session.State.Awaiting -> listener.onStateChanged(
					sessionId.toString(), RemoteSession.State.Awaiting.name
				)

				Session.State.Cancelled -> listener.onStateChanged(
					sessionId.toString(), RemoteSession.State.Cancelled.name
				)

				Session.State.Committed -> listener.onStateChanged(
					sessionId.toString(), RemoteSession.State.Committed.name
				)

				is Session.State.Failed -> listener.onFailed(
					sessionId.toString(), getFailure(state.failure).name
				)

				Session.State.Succeeded -> listener.onStateChanged(
					sessionId.toString(), RemoteSession.State.Succeeded.name
				)

				Session.State.Pending -> listener.onStateChanged(
					sessionId.toString(), RemoteSession.State.Pending.name
				)
			}
		}
		session.addStateListener(
			DisposableSubscriptionContainer(),
			stateListeners.putIfAbsent(listener, stateListener) ?: stateListener
		)
	}

	override fun removeStateListener(listener: ISessionStateListener) {
		val stateListener = stateListeners.remove(listener) ?: return
		session.removeStateListener(stateListener)
	}

	private fun getFailure(failure: Failure): RemoteFailure {
		if (failure is InstallFailure) {
			return when (failure) {
				is InstallFailure.Aborted -> RemoteFailure.Aborted
				is InstallFailure.Blocked -> RemoteFailure.Blocked
				is InstallFailure.Conflict -> RemoteFailure.Conflict
				is InstallFailure.Exceptional -> RemoteFailure.Exceptional
				is InstallFailure.Generic -> RemoteFailure.Generic
				is InstallFailure.Incompatible -> RemoteFailure.Incompatible
				is InstallFailure.Invalid -> RemoteFailure.Invalid
				is InstallFailure.Storage -> RemoteFailure.Storage
				is InstallFailure.Timeout -> RemoteFailure.Timeout
				else -> RemoteFailure.Unknown
			}
		}
		if (failure is UninstallFailure) {
			return when (failure) {
				is UninstallFailure.Aborted -> RemoteFailure.Aborted
				is UninstallFailure.Blocked -> RemoteFailure.Blocked
				is UninstallFailure.Conflict -> RemoteFailure.Conflict
				is UninstallFailure.Exceptional -> RemoteFailure.Exceptional
				is UninstallFailure.Generic -> RemoteFailure.Generic
				else -> RemoteFailure.Unknown
			}
		}
		return RemoteFailure.Unknown
	}
}