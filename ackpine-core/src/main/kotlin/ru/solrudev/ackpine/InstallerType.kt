package ru.solrudev.ackpine

import android.os.Build
import androidx.annotation.RequiresApi

public enum class InstallerType {
	INTENT_BASED,
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	SESSION_BASED;

	public companion object {

		@JvmField
		public val DEFAULT: InstallerType = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			INTENT_BASED
		} else {
			SESSION_BASED
		}
	}
}