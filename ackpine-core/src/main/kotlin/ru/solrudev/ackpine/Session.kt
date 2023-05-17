package ru.solrudev.ackpine

import java.util.UUID

public interface Session<F : Failure> {

	public val id: UUID
	public val isActive: Boolean
	public fun launch()
	public fun cancel()
	public fun addStateListener(listener: StateListener<F>): DisposableSubscription
	public fun removeStateListener(listener: StateListener<F>)

	public sealed interface State<F : Failure> {

		public sealed interface Terminal

		public val isTerminal: Boolean
			get() = this is Terminal

		public data object Pending : State<Nothing>
		public data object Active : State<Nothing>
		public data object Cancelled : State<Nothing>, Terminal
		public data object Succeeded : State<Nothing>, Terminal
		public data class Failed<F : Failure>(public val failure: F) : State<F>, Terminal
	}

	public fun interface StateListener<F : Failure> {
		public fun onStateChanged(sessionId: UUID, state: State<F>)
	}
}

internal class StateDisposableSubscription<F : Failure> internal constructor(
	private var session: Session<F>?,
	private var listener: Session.StateListener<F>?
) : DisposableSubscription {

	override var isDisposed: Boolean = false
		private set

	override fun dispose() {
		if (isDisposed) {
			return
		}
		val listener = this.listener
		if (listener != null) {
			session?.removeStateListener(listener)
		}
		this.listener = null
		session = null
		isDisposed = true
	}
}