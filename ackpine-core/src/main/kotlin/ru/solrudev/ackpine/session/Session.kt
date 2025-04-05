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
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
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
	 * Launches the session preparations. This includes copying needed files to temporary folder and other operations,
	 * like requesting [preapproval from user][InstallPreapproval].
	 *
	 * This method allows to re-launch the session when it's not in process of preparations and session's state hasn't
	 * reached [Awaiting] yet, e.g. when preparations were interrupted with process death.
	 *
	 * In general, this method should not be used directly, use [addStateListener] with [TerminalStateListener] or
	 * `await()` instead.
	 *
	 * @return `true` if session preparations have been launched due to this invocation. `false` if session preparations
	 * are in progress or have been already done, or session was cancelled.
	 */
	public fun launch(): Boolean

	/**
	 * Commits everything that was prepared in the session. This will possibly launch user's
	 * [confirmation][Confirmation] of installation or uninstallation.
	 *
	 * This method allows to re-commit the session when it's not in process of being committed or confirmed, e.g. when
	 * confirmation was interrupted with process death.
	 *
	 * When committing/confirmation is finished, session is considered [completed][isCompleted].
	 *
	 * In general, this method should not be used directly, use [addStateListener] with [TerminalStateListener] or
	 * `await()` instead.
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
	 * Consider using [TerminalStateListener.bind] instead of subclassing [TerminalStateListener].
	 *
	 * It's recommended to use this class with [Session.addStateListener] for listening to [terminal][State.isTerminal]
	 * state updates instead of bare [StateListener], because this class handles session's lifecycle appropriately.
	 */
	public abstract class TerminalStateListener<in F : Failure>(private val session: Session<F>) : StateListener<F> {

		/**
		 * Notifies that session was completed successfully.
		 * @param sessionId ID of the session which had its state updated.
		 */
		public open fun onSuccess(sessionId: UUID) { /* to be overridden */ }

		/**
		 * Notifies that session was completed with an error.
		 * @param sessionId ID of the session which had its state updated.
		 * @param failure session's failure cause.
		 */
		public open fun onFailure(sessionId: UUID, failure: F) { /* to be overridden */ }

		/**
		 * Notifies that session was cancelled.
		 * @param sessionId ID of the session which had its state updated.
		 */
		public open fun onCancelled(sessionId: UUID) { /* to be overridden */ }

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

		public companion object {

			/**
			 * Launches the [session] and handles its lifecycle. Returns a [Binder] to bind [terminal][State.isTerminal]
			 * state listeners to the session.
			 *
			 * If you don't want to receive terminal state updates, but instead just to launch the session, you can use
			 * the function like this:
			 *
			 * ```
			 * Session.TerminalStateListener.bind(session, subscriptions);
			 * ```
			 *
			 * @param session a [Session] to bind listeners to.
			 * @param subscriptionContainer a [subscription][DisposableSubscription] bag, session state subscription
			 * will be added to it.
			 * @return [Binder]
			 */
			@JvmStatic
			public fun <F : Failure> bind(
				session: Session<F>,
				subscriptionContainer: DisposableSubscriptionContainer
			): Binder<F> {
				val binder = Binder.create(session)
				session.addStateListener(subscriptionContainer, binder.terminalStateListener)
				return binder
			}
		}

		/**
		 * Allows to add terminal state listeners to a [Session].
		 */
		public class Binder<F : Failure> private constructor(session: Session<F>) {

			private var onSuccessListener: OnSuccessListener? = null
			private var onFailureListener: OnFailureListener<F>? = null
			private var onCancelListener: OnCancelListener? = null

			@get:JvmSynthetic
			internal val terminalStateListener = object : TerminalStateListener<F>(session) {

				override fun onSuccess(sessionId: UUID) {
					onSuccessListener?.onSuccess(sessionId)
				}

				override fun onFailure(sessionId: UUID, failure: F) {
					onFailureListener?.onFailure(sessionId, failure)
				}

				override fun onCancelled(sessionId: UUID) {
					onCancelListener?.onCancelled(sessionId)
				}
			}

			/**
			 * Adds a [listener] which will be invoked when a [Session] succeeds.
			 *
			 * Replaces previously added [OnSuccessListener].
			 *
			 * @return this [Binder] to allow chaining.
			 */
			public fun addOnSuccessListener(listener: OnSuccessListener): Binder<F> = apply {
				onSuccessListener = listener
			}

			/**
			 * Adds a [listener] which will be invoked when a [Session] fails.
			 *
			 * Replaces previously added [OnFailureListener].
			 *
			 * @return this [Binder] to allow chaining.
			 */
			public fun addOnFailureListener(listener: OnFailureListener<F>): Binder<F> = apply {
				onFailureListener = listener
			}

			/**
			 * Adds a [listener] which will be invoked when a [Session] is cancelled.
			 *
			 * Replaces previously added [OnCancelListener].
			 *
			 * @return this [Binder] to allow chaining.
			 */
			public fun addOnCancelListener(listener: OnCancelListener): Binder<F> = apply {
				onCancelListener = listener
			}

			internal companion object {
				@JvmSynthetic
				internal fun <F : Failure> create(session: Session<F>) = Binder(session)
			}
		}

		/**
		 * A listener which is invoked when a [Session] completes successfully.
		 */
		public fun interface OnSuccessListener {

			/**
			 * Invoked when a [Session] completes successfully.
			 * @param sessionId ID of the session which had its state updated.
			 */
			public fun onSuccess(sessionId: UUID)
		}

		/**
		 * A listener which is invoked when a [Session] completes with failure.
		 */
		public fun interface OnFailureListener<in F : Failure> {

			/**
			 * Invoked when a [Session] completes with failure.
			 * @param sessionId ID of the session which had its state updated.
			 * @param failure session's failure cause.
			 */
			public fun onFailure(sessionId: UUID, failure: F)
		}

		/**
		 * A listener which is invoked when a [Session] is cancelled.
		 */
		public fun interface OnCancelListener {

			/**
			 * Invoked when a [Session] is cancelled.
			 * @param sessionId ID of the session which had its state updated.
			 */
			public fun onCancelled(sessionId: UUID)
		}
	}
}