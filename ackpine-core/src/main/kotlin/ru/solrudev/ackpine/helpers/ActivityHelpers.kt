package ru.solrudev.ackpine.helpers

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.view.WindowManager

@Suppress("DEPRECATION")
@JvmSynthetic
internal fun Activity.turnScreenOnWhenLocked() {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
		setShowWhenLocked(true)
		setTurnScreenOn(true)
	} else {
		window.addFlags(
			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
					or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
					or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
		)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
		}
	}
	with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			requestDismissKeyguard(this@turnScreenOnWhenLocked, null)
		}
	}
}

@Suppress("DEPRECATION")
@JvmSynthetic
internal fun Activity.clearTurnScreenOnSettings() {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
		setShowWhenLocked(false)
		setTurnScreenOn(false)
	} else {
		window.clearFlags(
			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
					or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
					or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
		)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
		}
	}
}