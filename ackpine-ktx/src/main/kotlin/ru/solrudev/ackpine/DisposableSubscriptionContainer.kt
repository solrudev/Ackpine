package ru.solrudev.ackpine

@Suppress("NOTHING_TO_INLINE")
public inline operator fun DisposableSubscriptionContainer.plusAssign(subscription: DisposableSubscription) {
	add(subscription)
}