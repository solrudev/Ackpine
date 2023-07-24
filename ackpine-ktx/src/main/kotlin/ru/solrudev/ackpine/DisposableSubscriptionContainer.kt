package ru.solrudev.ackpine

/**
 * Adds the specified [subscription] to this [DisposableSubscriptionContainer].
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun DisposableSubscriptionContainer.plusAssign(subscription: DisposableSubscription) {
	add(subscription)
}