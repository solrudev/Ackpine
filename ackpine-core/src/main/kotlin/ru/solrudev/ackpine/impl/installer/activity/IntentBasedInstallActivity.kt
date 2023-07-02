package ru.solrudev.ackpine.impl.installer.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.installer.activity.helpers.getParcelableCompat
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session

private const val TAG = "IntentBasedInstallActivity"
private const val REQUEST_CODE = 1654101745

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class IntentBasedInstallActivity : InstallActivity(TAG, REQUEST_CODE) {

	private val apkUri by lazy {
		intent.extras?.getParcelableCompat<Uri>(APK_URI_KEY)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			launchInstallActivity()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode != REQUEST_CODE) {
			return
		}
		val result = if (resultCode == RESULT_OK) {
			Session.State.Succeeded
		} else {
			Session.State.Failed(InstallFailure.Generic())
		}
		withCompletableSession { session ->
			session?.complete(result)
		}
	}

	@Suppress("DEPRECATION")
	@SuppressLint("RestrictedApi")
	private fun launchInstallActivity() {
		if (apkUri == null) {
			withCompletableSession { session ->
				session?.completeExceptionally(
					IllegalStateException("$TAG: apkUri was null.")
				)
			}
			return
		}
		val intent = Intent().apply {
			action = Intent.ACTION_INSTALL_PACKAGE
			setDataAndType(apkUri, "application/vnd.android.package-archive")
			flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
			putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
			putExtra(Intent.EXTRA_RETURN_RESULT, true)
			putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, packageName)
		}
		startActivityForResult(intent, REQUEST_CODE)
		withCompletableSession { session ->
			session?.notifyCommitted()
		}
	}

	internal companion object {

		@get:JvmSynthetic
		internal const val APK_URI_KEY = "ACKPINE_INSTALLER_APK_URI"
	}
}