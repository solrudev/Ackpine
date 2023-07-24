package ru.solrudev.ackpine.session.parameters

import android.Manifest.permission.DISABLE_KEYGUARD
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.USE_FULL_SCREEN_INTENT
import android.Manifest.permission.VIBRATE
import ru.solrudev.ackpine.session.parameters.Confirmation.DEFERRED
import ru.solrudev.ackpine.session.parameters.Confirmation.IMMEDIATE

/**
 * A strategy for handling user's confirmation of installation or uninstallation.
 *
 * * [IMMEDIATE] &mdash; user will be prompted to confirm installation or uninstallation right away. Suitable for
 * 	 launching session directly from the UI when app is in foreground.
 * * [DEFERRED] (default) &mdash; user will be shown a high-priority notification (full-screen intent) which will launch
 *   confirmation activity.
 */
public enum class Confirmation {

	/**
	 * Prompt user to confirm installation or uninstallation right away.
	 */
	IMMEDIATE,

	/**
	 * Show a high-priority notification (full-screen intent) which will launch confirmation activity to a user.
	 *
	 * Requires [USE_FULL_SCREEN_INTENT], [DISABLE_KEYGUARD], [VIBRATE], [POST_NOTIFICATIONS] permissions.
	 */
	DEFERRED
}