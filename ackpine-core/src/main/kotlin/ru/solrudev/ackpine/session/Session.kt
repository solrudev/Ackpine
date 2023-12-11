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

package ru.solrudev.ackpine.session

import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session.TerminalStateListener
import ru.solrudev.ackpine.session.Session.State.Active
import ru.solrudev.ackpine.session.Session.State.Awaiting
import ru.solrudev.ackpine.session.Session.State.Cancelled
import ru.solrudev.ackpine.session.Session.State.Committed
import ru.solrudev.ackpine.session.Session.State.Failed
import ru.solrudev.ackpine.session.Session.State.Pending
import ru.solrudev.ackpine.session.Session.State.Succeeded
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID

/**
 * An installation or uninstallation that's being actively staged.
 *
 * The session by itself is passive, so to advance it an observer is needed to appropriately handle its lifecycle by
 * calling necessary methods.
 * [TerminalStateListener] is such an observer.
 *
 * @param F a type of [Failure] for this session, may be [InstallFailure] or [UninstallFailure].
 */
public interface Session<out F : Failure> {

	/**
	 * Returns the ID of this session.
	 */
	public val id: UUID

	/**
	 * Returns `true` if this session is active, i.e. it is already [launched][launch] and is not in
	 * [terminal][State.isTerminal] state.
	 */
	public val isActive: Boolean

	/**
	 * Returns `true` if this session has ran to completion without cancelling.
	 */
	public val isCompleted: Boolean

	/**
	 * Returns `true` if this session was cancelled by invocation of [cancel].
	 */
	public val isCancelled: Boolean

	/**
	 * Launches the session preparations. This includes copying needed files to temporary folder and other operations.
	 *
	 * This method allows to re-launch the session when it's not in process of preparations and session's state hasn't
	 * reached [Awaiting] yet, e.g. when preparations were interrupted with process death.
	 *
	 * @return `true` if session preparations have been launched due to this invocation. `false` if session preparations
	 * are in progress or have been already done, or session was cancelled.
	 */
	public fun launch(): Boolean

	/**
	 * Commits everything that was prepared in the session. This will launch user's [confirmation][Confirmation] of
	 * installation or uninstallation.
	 *
	 * This method allows to re-commit the session when it's not in process of being committed or confirmed, e.g. when
	 * confirmation was interrupted with process death.
	 *
	 * When committing/confirmation is finished, session is considered [completed][isCompleted].
	 *
	 * @return `true` if this session has been committed due to this invocation. `false` if committing/confirmation is
	 * in progress or has been already finished, or [session preparations][launch] hasn't been done beforehand, or
	 * session was cancelled.
	 */
	public fun commit(): Boolean

	/**
	 * Cancels this session, rendering it invalid.
	 */
	public fun cancel()

	/**
	 * Adds a [StateListener] to this session if it's not registered yet and appends the subscription to the
	 * [subscriptions bag][subscriptionContainer]. The listener will be notified with current state immediately upon
	 * registering.
	 *
	 * Listeners are notified on main thread.
	 *
	 * @return [DisposableSubscription] &mdash; a handle to the subscription, dummy object if listener is already
	 * registered.
	 */
	public fun addStateListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: StateListener<F>
	): DisposableSubscription

	/**
	 * Removes the provided [StateListener] from this session.
	 */
	public fun removeStateListener(listener: StateListener<F>)

	/**
	 * Represents the [session's][Session] state.
	 */
	public sealed interface State<out F : Failure> {

		/**
		 * Marks a session's state as [terminal][isTerminal].
		 */
		public sealed interface Terminal

		/**
		 *  Marks a session's state as [completed][isCompleted].
		 */
		public sealed interface Completed<out F : Failure> : State<F>

		/**
		 * Returns whether the session is in terminal state.
		 *
		 * [Cancelled], [Succeeded] and [Failed] are considered as terminal states.
		 */
		public val isTerminal: Boolean
			get() = this is Terminal

		/**
		 * Denotes that a session is not launched yet.
		 */
		public data object Pending : State<Nothing>

		/**
		 * Denotes that a session is being actively prepared.
		 */
		public data object Active : State<Nothing>

		/**
		 * Denotes that a session is prepared and is awaiting to be committed.
		 */
		public data object Awaiting : State<Nothing>

		/**
		 * Denotes that a session is committed.
		 */
		public data object Committed : State<Nothing>

		/**
		 * Denotes that a session is cancelled.
		 */
		public data object Cancelled : State<Nothing>, Terminal

		/**
		 * Denotes that a session is completed successfully.
		 */
		public data object Succeeded : State<Nothing>, Terminal, Completed<Nothing>

		/**
		 * Denotes that a session is completed with an error.
		 * @property failure session's failure cause.
		 */
		public data class Failed<out F : Failure>(public val failure: F) : State<F>, Terminal, Completed<F>
	}

	/**
	 * Callback interface for listening to [Session] state updates.
	 */
	public fun interface StateListener<in F : Failure> {

		/**
		 * Notifies about state update.
		 * @param sessionId ID of the session which had its state updated.
		 * @param state session's state.
		 */
		public fun onStateChanged(sessionId: UUID, state: State<F>)
	}

	/**
	 * A [StateListener] which implements [onStateChanged] method. This listener will call session's methods on state
	 * changes appropriately.
	 *
	 * Adding this listener to a session launches it if it's not already.
	 *
	 * It's recommended to use this class for listening to [terminal][State.isTerminal] state updates instead of bare
	 * [StateListener], because this class handles session's lifecycle appropriately.
	 */
	public abstract class TerminalStateListener<in F : Failure>(private val session: Session<F>) : StateListener<F> {

		/**
		 * Notifies that session was completed successfully.
		 * @param sessionId ID of the session which had its state updated.
		 */
		public open fun onSuccess(sessionId: UUID) {}

		/**
		 * Notifies that session was completed with an error.
		 * @param sessionId ID of the session which had its state updated.
		 * @param failure session's failure cause.
		 */
		public open fun onFailure(sessionId: UUID, failure: F) {}

		/**
		 * Notifies that session was cancelled.
		 * @param sessionId ID of the session which had its state updated.
		 */
		public open fun onCancelled(sessionId: UUID) {}

		final override fun onStateChanged(sessionId: UUID, state: State<F>) {
			if (state.isTerminal) {
				session.removeStateListener(this)
			}
			when (state) {
				Pending -> session.launch()
				Active -> session.launch() // re-launch if preparations were interrupted
				Awaiting -> session.commit()
				Committed -> session.commit() // re-commit if confirmation was interrupted
				Cancelled -> onCancelled(sessionId)
				Succeeded -> onSuccess(sessionId)
				is Failed -> onFailure(sessionId, state.failure)
			}
		}
	}
}