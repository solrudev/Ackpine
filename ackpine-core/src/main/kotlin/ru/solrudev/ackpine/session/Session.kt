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
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session.DefaultStateListener
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID

/**
 * An installation or uninstallation that's being actively staged.
 *
 * The session by itself is passive, so to advance it an observer is needed to appropriately handle its lifecycle by
 * calling necessary methods.
 * [DefaultStateListener] is such an observer.
 *
 * @param F a type of [Failure] for this session, may be [InstallFailure] or [UninstallFailure].
 */
public interface Session<out F : Failure> {

	/**
	 * Returns the ID of this session.
	 */
	public val id: UUID

	/**
	 * Returns whether this session is active, i.e. it is already [launched][launch] and is not in
	 * [terminal][State.isTerminal] state.
	 */
	public val isActive: Boolean

	/**
	 * Launches the session preparations. This includes copying needed files to temporary folder and other operations.
	 */
	public fun launch()

	/**
	 * Commits everything that was prepared in the session. This will launch user's [confirmation][Confirmation] of
	 * installation or uninstallation.
	 */
	public fun commit()

	/**
	 * Cancels this session, rendering it invalid.
	 */
	public fun cancel()

	/**
	 * Adds a [StateListener] to this session. The listener will be notified with current state immediately upon
	 * registering.
	 *
	 * Listeners are notified on main thread.
	 *
	 * @return [DisposableSubscription]
	 */
	public fun addStateListener(listener: StateListener<F>): DisposableSubscription

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
		 * Returns whether the session is in completed state.
		 *
		 * [Succeeded] and [Failed] are considered as completed states.
		 */
		public val isCompleted: Boolean
			get() = this is Completed

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
	public abstract class DefaultStateListener<in F : Failure>(private val session: Session<F>) : StateListener<F> {

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
				State.Pending -> session.launch()
				State.Active -> session.launch()
				State.Awaiting -> session.commit()
				State.Committed -> {}
				State.Cancelled -> onCancelled(sessionId)
				State.Succeeded -> onSuccess(sessionId)
				is State.Failed -> onFailure(sessionId, state.failure)
			}
		}
	}
}