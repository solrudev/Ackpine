package ru.solrudev.ackpine

public operator fun DisposableSubscriptionContainer.plusAssign(subscription: DisposableSubscription) {
	add(subscription)
}