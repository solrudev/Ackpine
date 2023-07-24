package ru.solrudev.ackpine

/**
 * A handle to a subscription via listener which can be disposed.
 */
public interface DisposableSubscription {

	/**
	 * Returns whether this subscription was disposed.
	 */
	public val isDisposed: Boolean

	/**
	 * Disposes this subscription and removes the listener from publisher.
	 */
	public fun dispose()
}

/**
 * A container for multiple [disposable subscriptions][DisposableSubscription].
 */
public class DisposableSubscriptionContainer : DisposableSubscription {

	override var isDisposed: Boolean = false
		private set

	private val subscriptions = mutableListOf<DisposableSubscription>()

	/**
	 * Adds the specified [subscription] to this [DisposableSubscriptionContainer].
	 */
	public fun add(subscription: DisposableSubscription) {
		if (!isDisposed) {
			subscriptions += subscription
		}
	}

	/**
	 * Clears this [DisposableSubscriptionContainer] disposing contained subscriptions, without disposing the container
	 * itself.
	 */
	public fun clear() {
		subscriptions.forEach { it.dispose() }
		subscriptions.clear()
	}

	public override fun dispose() {
		if (!isDisposed) {
			clear()
			isDisposed = true
		}
	}
}